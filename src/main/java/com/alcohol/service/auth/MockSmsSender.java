package com.alcohol.service.auth;

import com.alcohol.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alcohol.auth.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

    private final AuthProperties authProperties;

    @Override
    public void sendOtp(String phoneE164, String code, String locale) {
        log.info("[MOCK SMS] to={} code={} locale={} (dev only, configure alcohol.auth.sms.provider=aws for production)",
                phoneE164, code, locale);
    }
}
