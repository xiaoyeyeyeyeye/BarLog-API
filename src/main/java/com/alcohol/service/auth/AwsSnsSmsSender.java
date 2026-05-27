package com.alcohol.service.auth;

import com.alcohol.common.BizException;
import com.alcohol.config.AuthProperties;
import com.alcohol.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alcohol.auth.sms.provider", havingValue = "aws")
public class AwsSnsSmsSender implements SmsSender {

    private final AwsProperties awsProperties;
    private final AuthProperties authProperties;

    @Override
    public void sendOtp(String phoneE164, String code, String locale) {
        String message = buildMessage(code, locale);
        try (SnsClient sns = SnsClient.builder().region(Region.of(awsProperties.getRegion())).build()) {
            sns.publish(PublishRequest.builder()
                    .phoneNumber(phoneE164)
                    .message(message)
                    .build());
            log.info("SMS OTP sent via AWS SNS to {}", maskPhone(phoneE164));
        } catch (Exception e) {
            log.error("AWS SNS send failed", e);
            throw new BizException("短信发送失败，请稍后重试");
        }
    }

    private String buildMessage(String code, String locale) {
        if (locale != null && locale.startsWith("zh")) {
            return "【BarLog】您的验证码是 " + code + "，" + authProperties.getOtp().getTtlMinutes() + " 分钟内有效。";
        }
        return "[BarLog] Your verification code is " + code + ". Valid for "
                + authProperties.getOtp().getTtlMinutes() + " minutes.";
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 4) return "****";
        return phone.substring(0, Math.min(4, phone.length())) + "****";
    }
}
