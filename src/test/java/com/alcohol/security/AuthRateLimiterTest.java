package com.alcohol.security;

import com.alcohol.common.BizException;
import com.alcohol.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthRateLimiterTest {

    private AuthRateLimiter limiter;

    @BeforeEach
    void setUp() {
        SecurityProperties props = new SecurityProperties();
        props.getRateLimit().setEnabled(true);
        props.getRateLimit().setMaxFailedLoginsPerIp(3);
        props.getRateLimit().setFailedLoginWindowMinutes(15);
        limiter = new AuthRateLimiter(props);
    }

    @Test
    void blocksAfterRepeatedFailedLogins() {
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> limiter.recordFailedLogin("1.2.3.4"));
        }
        assertThrows(BizException.class, () -> limiter.recordFailedLogin("1.2.3.4"));
    }
}
