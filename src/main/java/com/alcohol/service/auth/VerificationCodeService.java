package com.alcohol.service.auth;

import com.alcohol.common.BizException;
import com.alcohol.config.AuthProperties;
import com.alcohol.entity.VerificationCode;
import com.alcohol.enums.VerificationChannel;
import com.alcohol.enums.VerificationPurpose;
import com.alcohol.mapper.VerificationCodeMapper;
import com.alcohol.util.PhoneEmailUtil;
import com.alcohol.vo.auth.OtpSendVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private final VerificationCodeMapper verificationCodeMapper;
    private final AuthProperties authProperties;
    private final List<SmsSender> smsSenders;
    private final List<EmailSender> emailSenders;

    @Transactional
    public OtpSendVO send(String channelRaw, String purposeRaw, String target, String countryCode) {
        VerificationChannel channel = parseChannel(channelRaw);
        VerificationPurpose purpose = parsePurpose(purposeRaw);
        String normalizedTarget = normalizeTarget(channel, target, countryCode);

        checkCooldown(normalizedTarget, purpose.name());
        checkDailyLimit(normalizedTarget);

        String code = PhoneEmailUtil.randomDigits(authProperties.getOtp().getLength());
        VerificationCode record = new VerificationCode();
        record.setTarget(normalizedTarget);
        record.setChannel(channel.name());
        record.setPurpose(purpose.name());
        record.setCodeHash(PhoneEmailUtil.sha256(code + ":" + normalizedTarget));
        record.setCountryCode(countryCode);
        record.setExpiresAt(LocalDateTime.now().plusMinutes(authProperties.getOtp().getTtlMinutes()));
        record.setAttemptCount(0);
        record.setCreatedAt(LocalDateTime.now());
        verificationCodeMapper.insert(record);

        dispatch(channel, normalizedTarget, code);

        OtpSendVO vo = new OtpSendVO();
        vo.setChannel(channel.name());
        vo.setTargetMasked(maskTarget(normalizedTarget, channel));
        vo.setExpiresInSeconds(authProperties.getOtp().getTtlMinutes() * 60);
        vo.setCooldownSeconds(authProperties.getOtp().getCooldownSeconds());
        if ("mock".equals(getProvider(channel))) {
            vo.setDebugCode(code);
        }
        return vo;
    }

    @Transactional
    public void verify(String channelRaw, String purposeRaw, String target, String countryCode, String code) {
        VerificationChannel channel = parseChannel(channelRaw);
        VerificationPurpose purpose = parsePurpose(purposeRaw);
        String normalizedTarget = normalizeTarget(channel, target, countryCode);

        VerificationCode latest = verificationCodeMapper.selectOne(new LambdaQueryWrapper<VerificationCode>()
                .eq(VerificationCode::getTarget, normalizedTarget)
                .eq(VerificationCode::getPurpose, purpose.name())
                .isNull(VerificationCode::getConsumedAt)
                .gt(VerificationCode::getExpiresAt, LocalDateTime.now())
                .orderByDesc(VerificationCode::getCreatedAt)
                .last("LIMIT 1"));

        if (latest == null) {
            throw new BizException("验证码已过期或不存在，请重新获取");
        }
        if (latest.getAttemptCount() >= authProperties.getOtp().getMaxAttempts()) {
            throw new BizException("验证码尝试次数过多，请重新获取");
        }

        latest.setAttemptCount(latest.getAttemptCount() + 1);
        String hash = PhoneEmailUtil.sha256(code + ":" + normalizedTarget);
        if (!hash.equals(latest.getCodeHash())) {
            verificationCodeMapper.updateById(latest);
            throw new BizException("验证码错误");
        }
        latest.setConsumedAt(LocalDateTime.now());
        verificationCodeMapper.updateById(latest);
    }

    private void dispatch(VerificationChannel channel, String target, String code) {
        String locale = authProperties.getDefaultLocale();
        if (channel == VerificationChannel.SMS) {
            smsSenders.get(0).sendOtp(target, code, locale);
        } else {
            emailSenders.get(0).sendOtp(target, code, locale);
        }
    }

    private String getProvider(VerificationChannel channel) {
        return channel == VerificationChannel.SMS
                ? authProperties.getSms().getProvider()
                : authProperties.getEmail().getProvider();
    }

    private void checkCooldown(String target, String purpose) {
        VerificationCode recent = verificationCodeMapper.selectOne(new LambdaQueryWrapper<VerificationCode>()
                .eq(VerificationCode::getTarget, target)
                .eq(VerificationCode::getPurpose, purpose)
                .orderByDesc(VerificationCode::getCreatedAt)
                .last("LIMIT 1"));
        if (recent != null && recent.getCreatedAt().plusSeconds(authProperties.getOtp().getCooldownSeconds())
                .isAfter(LocalDateTime.now())) {
            throw new BizException("发送过于频繁，请 " + authProperties.getOtp().getCooldownSeconds() + " 秒后再试");
        }
    }

    private void checkDailyLimit(String target) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        Long count = verificationCodeMapper.selectCount(new LambdaQueryWrapper<VerificationCode>()
                .eq(VerificationCode::getTarget, target)
                .ge(VerificationCode::getCreatedAt, start));
        if (count >= authProperties.getOtp().getDailyLimitPerTarget()) {
            throw new BizException("今日验证码发送次数已达上限");
        }
    }

    private String normalizeTarget(VerificationChannel channel, String target, String countryCode) {
        return channel == VerificationChannel.SMS
                ? PhoneEmailUtil.normalizePhoneE164(countryCodeOrDefault(countryCode), target)
                : PhoneEmailUtil.normalizeEmail(target);
    }

    private String countryCodeOrDefault(String countryCode) {
        return StringUtils.hasText(countryCode) ? countryCode : authProperties.getDefaultCountryCode();
    }

    private VerificationChannel parseChannel(String raw) {
        try {
            return VerificationChannel.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            throw new BizException("无效的 channel，应为 SMS 或 EMAIL");
        }
    }

    private VerificationPurpose parsePurpose(String raw) {
        try {
            return VerificationPurpose.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            throw new BizException("无效的 purpose");
        }
    }

    private String maskTarget(String target, VerificationChannel channel) {
        if (channel == VerificationChannel.EMAIL) {
            int at = target.indexOf('@');
            return at > 1 ? target.charAt(0) + "***" + target.substring(at) : "***";
        }
        return target.length() > 4 ? target.substring(0, 4) + "****" : "****";
    }
}
