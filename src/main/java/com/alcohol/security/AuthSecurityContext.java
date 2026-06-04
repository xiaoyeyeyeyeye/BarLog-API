package com.alcohol.security;

import com.alcohol.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class AuthSecurityContext {

    private final ClientIpResolver clientIpResolver;
    private final AuthRateLimiter rateLimiter;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final SecurityProperties securityProperties;

    public String clientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        return clientIp(clientIpResolver.resolve(attrs.getRequest()));
    }

    public String clientIp(HttpServletRequest request) {
        return clientIp(clientIpResolver.resolve(request));
    }

    public void validatePassword(String rawPassword) {
        passwordPolicyValidator.validate(rawPassword);
    }

    public void beforeRegister() {
        rateLimiter.checkRegistration(clientIp());
    }

    public void onLoginFailed() {
        rateLimiter.recordFailedLogin(clientIp());
    }

    public boolean isRateLimitEnabled() {
        return securityProperties.getRateLimit().isEnabled();
    }

    private static String clientIp(String ip) {
        return ip == null || ip.isBlank() ? "unknown" : ip;
    }
}
