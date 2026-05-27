package com.alcohol.service.auth;

import com.alcohol.common.BizException;
import com.alcohol.config.AuthProperties;
import com.alcohol.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alcohol.auth.email.provider", havingValue = "aws")
public class AwsSesEmailSender implements EmailSender {

    private final AwsProperties awsProperties;
    private final AuthProperties authProperties;

    @Override
    public void sendOtp(String email, String code, String locale) {
        String subject = locale != null && locale.startsWith("zh")
                ? "BarLog 验证码" : "BarLog Verification Code";
        String body = locale != null && locale.startsWith("zh")
                ? "您的验证码是 " + code + "，" + authProperties.getOtp().getTtlMinutes() + " 分钟内有效。"
                : "Your verification code is " + code + ". Valid for "
                + authProperties.getOtp().getTtlMinutes() + " minutes.";

        try (SesClient ses = SesClient.builder().region(Region.of(awsProperties.getRegion())).build()) {
            ses.sendEmail(SendEmailRequest.builder()
                    .source(authProperties.getEmail().getFromAddress())
                    .destination(Destination.builder().toAddresses(email).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .text(Content.builder().data(body).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build());
            log.info("Email OTP sent via AWS SES to {}", maskEmail(email));
        } catch (Exception e) {
            log.error("AWS SES send failed", e);
            throw new BizException("邮件发送失败，请稍后重试");
        }
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
