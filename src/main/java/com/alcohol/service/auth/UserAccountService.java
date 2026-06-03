package com.alcohol.service.auth;

import com.alcohol.common.BizException;
import com.alcohol.config.AuthProperties;
import com.alcohol.entity.User;
import com.alcohol.entity.UserAuthProvider;
import com.alcohol.enums.AuthProvider;
import com.alcohol.mapper.UserAuthProviderMapper;
import com.alcohol.mapper.UserMapper;
import com.alcohol.util.JsonUtil;
import com.alcohol.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户账号创建、OAuth 绑定、handle 生成。
 */
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserMapper userMapper;
    private final UserAuthProviderMapper userAuthProviderMapper;
    private final PasswordUtil passwordUtil;
    private final AuthProperties authProperties;

    public User findByPhoneE164(String phoneE164) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phoneE164));
    }

    public User findByEmail(String email) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    }

    public User findByOAuth(String provider, String providerUserId) {
        UserAuthProvider link = userAuthProviderMapper.selectOne(new LambdaQueryWrapper<UserAuthProvider>()
                .eq(UserAuthProvider::getProvider, provider)
                .eq(UserAuthProvider::getProviderUserId, providerUserId));
        if (link == null) return null;
        return userMapper.selectById(link.getUserId());
    }

    @Transactional
    public User createPhoneUser(String phoneE164, String countryCode, String password, String nickname) {
        ensurePhoneAvailable(phoneE164);
        User user = baseUser(nickname, AuthProvider.PHONE);
        user.setPhone(phoneE164);
        user.setCountryCode(countryCode);
        user.setPhoneVerified(StringUtils.hasText(password) ? 0 : 1);
        if (StringUtils.hasText(password)) {
            user.setPassword(passwordUtil.encode(password));
        }
        persistUser(user);
        return user;
    }

    @Transactional
    public User createEmailUser(String email, String password, String nickname) {
        ensureEmailAvailable(email);
        User user = baseUser(nickname, AuthProvider.EMAIL);
        user.setEmail(email);
        user.setEmailVerified(1);
        user.setPassword(passwordUtil.encode(password));
        persistUser(user);
        return user;
    }

    @Transactional
    public User createOtpUser(String phoneE164, String countryCode, String nickname, AuthProvider provider) {
        User existing = provider == AuthProvider.EMAIL
                ? findByEmail(phoneE164)
                : findByPhoneE164(phoneE164);
        if (existing != null) return existing;

        User user = baseUser(nickname, provider);
        if (provider == AuthProvider.EMAIL) {
            user.setEmail(phoneE164);
            user.setEmailVerified(1);
        } else {
            user.setPhone(phoneE164);
            user.setCountryCode(countryCode);
            user.setPhoneVerified(1);
        }
        persistUser(user);
        return user;
    }

    @Transactional
    public OAuthLoginResult findOrCreateOAuthUser(OAuthUserInfo info, AuthProvider provider, String nicknameOverride) {
        User linked = findByOAuth(provider.name(), info.getProviderUserId());
        if (linked != null) {
            assertActive(linked);
            return new OAuthLoginResult(linked, false);
        }

        User user = null;
        if (StringUtils.hasText(info.getEmail())) {
            user = findByEmail(info.getEmail());
        }
        boolean isNew = false;
        if (user == null) {
            String nickname = StringUtils.hasText(nicknameOverride) ? nicknameOverride
                    : (StringUtils.hasText(info.getName()) ? info.getName() : "BarLog User");
            user = baseUser(nickname, provider);
            if (StringUtils.hasText(info.getEmail())) {
                user.setEmail(info.getEmail());
                user.setEmailVerified(1);
            }
            if (StringUtils.hasText(info.getAvatarUrl())) {
                user.setAvatarUrl(info.getAvatarUrl());
            }
            persistUser(user);
            isNew = true;
        } else {
            assertActive(user);
        }

        linkOAuth(user.getId(), info, provider);
        return new OAuthLoginResult(user, isNew);
    }

    @Transactional
    public User loginOAuthUser(OAuthUserInfo info, AuthProvider provider) {
        User linked = findByOAuth(provider.name(), info.getProviderUserId());
        if (linked != null) {
            assertActive(linked);
            return linked;
        }
        if (StringUtils.hasText(info.getEmail())) {
            User byEmail = findByEmail(info.getEmail());
            if (byEmail != null) {
                assertActive(byEmail);
                linkOAuth(byEmail.getId(), info, provider);
                return byEmail;
            }
        }
        throw new BizException("No BarLog account linked to this Google identity", 404);
    }

    public record OAuthLoginResult(User user, boolean newUser) {}

    @Transactional
    public void linkOAuth(String userId, OAuthUserInfo info, AuthProvider provider) {
        UserAuthProvider existing = userAuthProviderMapper.selectOne(new LambdaQueryWrapper<UserAuthProvider>()
                .eq(UserAuthProvider::getProvider, provider.name())
                .eq(UserAuthProvider::getProviderUserId, info.getProviderUserId()));
        if (existing != null) return;

        UserAuthProvider link = new UserAuthProvider();
        link.setUserId(userId);
        link.setProvider(provider.name());
        link.setProviderUserId(info.getProviderUserId());
        link.setProviderEmail(info.getEmail());
        link.setDisplayName(info.getName());
        link.setAvatarUrl(info.getAvatarUrl());
        link.setRawProfile(info.getRawProfile());
        link.setLinkedAt(LocalDateTime.now());
        link.setUpdatedAt(LocalDateTime.now());
        userAuthProviderMapper.insert(link);
    }

    public void assertActive(User user) {
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
    }

    private User baseUser(String nickname, AuthProvider provider) {
        User user = new User();
        user.setNickname(nickname);
        user.setGender("UNSPECIFIED");
        user.setPrivacySettings(JsonUtil.toJson(defaultPrivacy()));
        user.setSocialPreferences("{}");
        user.setTonightEnabled(0);
        user.setProfileBgTheme(0);
        user.setSpotifyConnected(0);
        user.setSpotifyGenres("[]");
        user.setAvatarEmoji("🍸");
        user.setCountryCode(authProperties.getDefaultCountryCode());
        user.setLocale(authProperties.getDefaultLocale());
        user.setPrimaryAuthProvider(provider.name());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private void persistUser(User user) {
        userMapper.insert(user);
        user.setHandle(generateHandle(user));
        userMapper.updateById(user);
    }

    private void ensurePhoneAvailable(String phone) {
        if (findByPhoneE164(phone) != null) {
            throw new BizException("该手机号已注册");
        }
    }

    private void ensureEmailAvailable(String email) {
        if (findByEmail(email) != null) {
            throw new BizException("该邮箱已注册");
        }
    }

    private String generateHandle(User user) {
        String base = "barlog_" + user.getId().replace("-", "").substring(0, 8).toLowerCase();
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getHandle, base));
        if (count == 0) return base;
        String suffix = StringUtils.hasText(user.getPhone())
                ? user.getPhone().substring(Math.max(0, user.getPhone().length() - 4))
                : user.getId().substring(0, 4);
        return base + "_" + suffix;
    }

    private Map<String, Boolean> defaultPrivacy() {
        return Map.of(
                "showHistoryCards", true,
                "showCityMap", true,
                "showFrequentArea", false,
                "allowStrangerDm", false,
                "sameBarOnly", false,
                "sameGenderOnly", false,
                "hideExactLocation", true
        );
    }
}
