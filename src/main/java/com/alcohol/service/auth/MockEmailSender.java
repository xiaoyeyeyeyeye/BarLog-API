package com.alcohol.service.auth;

import com.alcohol.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alcohol.auth.email.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailSender implements EmailSender {

    @Override
    public void sendOtp(String email, String code, String locale) {
        log.info("[MOCK EMAIL] to={} code={} locale={} (dev only, configure alcohol.auth.email.provider=aws for production)",
                email, code, locale);
    }

    @Override
    public void sendWelcome(String email, String displayName, String locale) {
        log.info("[MOCK EMAIL] welcome to={} name={} locale={}", email, displayName, locale);
    }
}
