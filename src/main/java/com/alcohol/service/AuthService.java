package com.alcohol.service;

import com.alcohol.common.BizException;
import com.alcohol.config.AuthProperties;
import com.alcohol.dto.auth.*;
import com.alcohol.entity.User;
import com.alcohol.enums.AuthProvider;
import com.alcohol.enums.VerificationChannel;
import com.alcohol.enums.VerificationPurpose;
import com.alcohol.service.auth.OAuthTokenVerifier;
import com.alcohol.service.auth.OAuthUserInfo;
import com.alcohol.service.auth.UserAccountService;
import com.alcohol.service.auth.VerificationCodeService;
import com.alcohol.util.JwtUtil;
import com.alcohol.util.PasswordUtil;
import com.alcohol.util.PhoneEmailUtil;
import com.alcohol.vo.auth.AuthMethodsVO;
import com.alcohol.vo.auth.LoginVO;
import com.alcohol.vo.auth.OtpSendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 海外认证入口：邮箱、手机 OTP、Google、Facebook、密码登录。
 * <p>默认区域新加坡（+65 / en-SG），短信/邮件生产环境走 AWS SNS/SES。</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountService userAccountService;
    private final VerificationCodeService verificationCodeService;
    private final OAuthTokenVerifier oauthTokenVerifier;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final AuthProperties authProperties;

    public AuthMethodsVO supportedMethods() {
        AuthMethodsVO vo = new AuthMethodsVO();
        vo.setDefaultCountryCode(authProperties.getDefaultCountryCode());
        vo.setDefaultLocale(authProperties.getDefaultLocale());
        vo.setSupportedCountryCodes(List.of("+65", "+60", "+62", "+66", "+84", "+86", "+1"));
        vo.setMethods(List.of(
                method("PHONE_OTP", "Phone OTP", "SMS verification, recommended for SG (+65)"),
                method("EMAIL", "Email", "Email + password or OTP"),
                method("GOOGLE", "Google", "Sign in with Google"),
                method("FACEBOOK", "Facebook", "Sign in with Facebook"),
                method("PHONE_PASSWORD", "Phone + Password", "Legacy password login")
        ));
        return vo;
    }

    public OtpSendVO sendOtp(SendOtpRequest req) {
        return verificationCodeService.send(req.getChannel(), req.getPurpose(), req.getTarget(), req.getCountryCode());
    }

    @Transactional
    public LoginVO loginWithOtp(OtpLoginRequest req) {
        verificationCodeService.verify(req.getChannel(), req.getPurpose(), req.getTarget(), req.getCountryCode(), req.getCode());

        VerificationChannel channel = VerificationChannel.valueOf(req.getChannel().toUpperCase());
        boolean isNew = false;
        User user;

        if (channel == VerificationChannel.SMS) {
            String cc = StringUtils.hasText(req.getCountryCode()) ? req.getCountryCode() : authProperties.getDefaultCountryCode();
            String phone = PhoneEmailUtil.normalizePhoneE164(cc, req.getTarget());
            user = userAccountService.findByPhoneE164(phone);
            if (user == null) {
                requireNickname(req.getNickname());
                user = userAccountService.createOtpUser(phone, cc, req.getNickname(), AuthProvider.PHONE);
                isNew = true;
            }
        } else {
            String email = PhoneEmailUtil.normalizeEmail(req.getTarget());
            user = userAccountService.findByEmail(email);
            if (user == null) {
                requireNickname(req.getNickname());
                user = userAccountService.createOtpUser(email, null, req.getNickname(), AuthProvider.EMAIL);
                isNew = true;
            }
        }

        userAccountService.assertActive(user);
        return buildLoginVO(user, isNew);
    }

    @Transactional
    public LoginVO registerEmail(EmailRegisterRequest req) {
        String email = PhoneEmailUtil.normalizeEmail(req.getEmail());
        verificationCodeService.verify("EMAIL", VerificationPurpose.REGISTER.name(), email, null, req.getOtpCode());
        User user = userAccountService.createEmailUser(email, req.getPassword(), req.getNickname());
        return buildLoginVO(user, true);
    }

    public LoginVO loginEmail(EmailLoginRequest req) {
        String email = PhoneEmailUtil.normalizeEmail(req.getEmail());
        User user = userAccountService.findByEmail(email);
        if (user == null || !StringUtils.hasText(user.getPassword())
                || !passwordUtil.matches(req.getPassword(), user.getPassword())) {
            throw new BizException("邮箱或密码错误");
        }
        userAccountService.assertActive(user);
        return buildLoginVO(user, false);
    }

    @Transactional
    public LoginVO loginGoogle(GoogleLoginRequest req) {
        OAuthUserInfo info = oauthTokenVerifier.verifyGoogle(req.getIdToken());
        var result = userAccountService.findOrCreateOAuthUser(info, AuthProvider.GOOGLE, req.getNickname());
        return buildLoginVO(result.user(), result.newUser());
    }

    @Transactional
    public LoginVO loginFacebook(FacebookLoginRequest req) {
        OAuthUserInfo info = oauthTokenVerifier.verifyFacebook(req.getAccessToken());
        var result = userAccountService.findOrCreateOAuthUser(info, AuthProvider.FACEBOOK, req.getNickname());
        return buildLoginVO(result.user(), result.newUser());
    }

    /** @deprecated 兼容旧接口，请用 {@link #registerPhone(PhoneRegisterRequest)} */
    @Transactional
    public LoginVO register(RegisterRequest req) {
        PhoneRegisterRequest pr = new PhoneRegisterRequest();
        pr.setPhone(req.getPhone());
        pr.setCountryCode(req.getCountryCode());
        pr.setPassword(req.getPassword());
        pr.setNickname(req.getNickname());
        return registerPhone(pr);
    }

    @Transactional
    public LoginVO registerPhone(PhoneRegisterRequest req) {
        String cc = StringUtils.hasText(req.getCountryCode()) ? req.getCountryCode() : authProperties.getDefaultCountryCode();
        String phone = PhoneEmailUtil.normalizePhoneE164(cc, req.getPhone());
        User user = userAccountService.createPhoneUser(phone, cc, req.getPassword(), req.getNickname());
        return buildLoginVO(user, true);
    }

    /** @deprecated 兼容旧接口，请用 {@link #loginPhone(PhoneLoginRequest)} */
    public LoginVO login(LoginRequest req) {
        PhoneLoginRequest pl = new PhoneLoginRequest();
        pl.setPhone(req.getPhone());
        pl.setCountryCode(guessCountryCode(req.getPhone()));
        pl.setPassword(req.getPassword());
        return loginPhone(pl);
    }

    private String guessCountryCode(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.matches("^1\\d{10}$")) {
            return "+86";
        }
        return authProperties.getDefaultCountryCode();
    }

    public LoginVO loginPhone(PhoneLoginRequest req) {
        String cc = StringUtils.hasText(req.getCountryCode()) ? req.getCountryCode() : authProperties.getDefaultCountryCode();
        String phone = PhoneEmailUtil.normalizePhoneE164(cc, req.getPhone());
        User user = userAccountService.findByPhoneE164(phone);
        if (user == null || !StringUtils.hasText(user.getPassword())
                || !passwordUtil.matches(req.getPassword(), user.getPassword())) {
            throw new BizException("手机号或密码错误");
        }
        userAccountService.assertActive(user);
        return buildLoginVO(user, false);
    }

    private LoginVO buildLoginVO(User user, boolean isNew) {
        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setToken(jwtUtil.generateToken(user.getId()));
        vo.setNewUser(isNew);
        vo.setAuthProvider(user.getPrimaryAuthProvider());
        return vo;
    }

    private void requireNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            throw new BizException("新用户请填写昵称");
        }
    }

    private AuthMethodsVO.MethodItem method(String code, String name, String desc) {
        AuthMethodsVO.MethodItem m = new AuthMethodsVO.MethodItem();
        m.setCode(code);
        m.setName(name);
        m.setDescription(desc);
        return m;
    }
}
