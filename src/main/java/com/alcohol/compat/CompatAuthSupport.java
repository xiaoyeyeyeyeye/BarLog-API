package com.alcohol.compat;

import com.alcohol.common.BizException;
import com.alcohol.config.CompatProperties;
import com.alcohol.context.UserContext;
import com.alcohol.entity.User;
import com.alcohol.service.auth.UserAccountService;
import com.alcohol.util.JwtUtil;
import com.alcohol.util.PasswordUtil;
import com.alcohol.util.PhoneEmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 开发阶段匿名回落：当前端未携带 Token 时，使用 demo 账号填充 {@link UserContext}。
 */
@Component
@RequiredArgsConstructor
public class CompatAuthSupport {

    private final CompatProperties compatProperties;
    private final UserAccountService userAccountService;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;

    public boolean tryAnonymousAuth() {
        if (!compatProperties.isAllowAnonymous()) {
            return false;
        }
        User demo = ensureDemoUser();
        UserContext.setUserId(demo.getId());
        return true;
    }

    public User ensureDemoUser() {
        String email = PhoneEmailUtil.normalizeEmail(compatProperties.getDemoUserEmail());
        User user = userAccountService.findByEmail(email);
        if (user != null) {
            return user;
        }
        return userAccountService.createEmailUser(
                email,
                compatProperties.getDemoUserPassword(),
                compatProperties.getDemoUserDisplayName());
    }

    public String issueAccessToken(User user) {
        return jwtUtil.generateToken(user.getId());
    }

    public String issueRefreshToken(User user) {
        return jwtUtil.generateToken(user.getId()) + ".refresh";
    }

    public boolean validateAccessToken(String token) {
        return jwtUtil.validateToken(token) && !jwtUtil.isTokenExpired(token);
    }

    public String userIdFromToken(String token) {
        return jwtUtil.getUserIdFromToken(token);
    }

    public void assertPassword(User user, String rawPassword) {
        if (user == null || !StringUtils.hasText(user.getPassword())
                || !passwordUtil.matches(rawPassword, user.getPassword())) {
            throw new BizException("Invalid email or password", 401);
        }
    }
}
