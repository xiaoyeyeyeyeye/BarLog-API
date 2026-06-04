package com.alcohol.security;

import com.alcohol.common.BizException;
import com.alcohol.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PasswordPolicyValidator {

    private final SecurityProperties securityProperties;

    public void validate(String rawPassword) {
        SecurityProperties.Password policy = securityProperties.getPassword();
        if (!policy.isEnforcePolicy()) {
            return;
        }
        if (!StringUtils.hasText(rawPassword) || rawPassword.length() < policy.getMinLength()) {
            throw new BizException("Password must be at least " + policy.getMinLength() + " characters", 400);
        }
        if (policy.isRequireLetter() && !rawPassword.matches(".*[A-Za-z].*")) {
            throw new BizException("Password must contain at least one letter", 400);
        }
        if (policy.isRequireDigit() && !rawPassword.matches(".*\\d.*")) {
            throw new BizException("Password must contain at least one digit", 400);
        }
    }
}
