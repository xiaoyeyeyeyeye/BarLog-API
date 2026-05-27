package com.alcohol.service;

import com.alcohol.common.BizException;
import com.alcohol.context.UserContext;
import com.alcohol.dto.user.PrivacySettingsRequest;
import com.alcohol.dto.user.TonightModeRequest;
import com.alcohol.dto.user.UpdateProfileRequest;
import com.alcohol.entity.CheckIn;
import com.alcohol.entity.User;
import com.alcohol.mapper.CheckInMapper;
import com.alcohol.mapper.UserMapper;
import com.alcohol.util.CheckInStatsUtil;
import com.alcohol.util.JsonUtil;
import com.alcohol.vo.user.UserProfileVO;
import com.alcohol.vo.user.UserStatsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 用户资料与统计。
 * <p>「我的」Tab：资料读写、隐私、Tonight Mode、打卡统计聚合。</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final CheckInMapper checkInMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserProfileVO getMe() {
        return toProfile(requireCurrentUser());
    }

    public UserProfileVO getPublicProfile(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在", 404);
        }
        return toProfile(user);
    }

    public UserStatsVO getStats() {
        return computeStats(requireCurrentUser().getId());
    }

    public UserStatsVO computeStats(String userId) {
        List<CheckIn> all = checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId));
        UserStatsVO stats = new UserStatsVO();
        stats.setCheckInCount(all.size());
        stats.setBarsVisited(CheckInStatsUtil.distinctBars(all));
        stats.setCardCount(CheckInStatsUtil.cardCount(all));
        stats.setAvgRating(CheckInStatsUtil.avgRating(all));
        return stats;
    }

    @Transactional
    public UserProfileVO updateProfile(UpdateProfileRequest req) {
        User user = requireCurrentUser();
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        if (req.getAvatarEmoji() != null) user.setAvatarEmoji(req.getAvatarEmoji());
        if (req.getProfileBgTheme() != null) user.setProfileBgTheme(req.getProfileBgTheme());
        if (req.getCity() != null) user.setCity(req.getCity());
        if (req.getBio() != null) user.setBio(req.getBio());
        if (req.getGender() != null) user.setGender(req.getGender());
        if (req.getMbti() != null) user.setMbti(req.getMbti());
        if (req.getFrequentArea() != null) user.setFrequentArea(req.getFrequentArea());
        if (req.getFavoriteDrink() != null) user.setFavoriteDrink(req.getFavoriteDrink());
        if (req.getSpotifyConnected() != null) {
            user.setSpotifyConnected(Boolean.TRUE.equals(req.getSpotifyConnected()) ? 1 : 0);
        }
        if (req.getSpotifyGenres() != null) {
            user.setSpotifyGenres(JsonUtil.toJson(req.getSpotifyGenres()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toProfile(user);
    }

    @Transactional
    public UserProfileVO updatePrivacy(PrivacySettingsRequest req) {
        User user = requireCurrentUser();
        user.setPrivacySettings(JsonUtil.toJson(req));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toProfile(user);
    }

    @Transactional
    public UserProfileVO updateTonightMode(TonightModeRequest req) {
        User user = requireCurrentUser();
        user.setTonightEnabled(Boolean.TRUE.equals(req.getEnabled()) ? 1 : 0);
        if (req.getSocialStatus() != null) {
            user.setTonightSocialStatus(req.getSocialStatus());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toProfile(user);
    }

    public User requireCurrentUser() {
        String userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException("未登录", 401);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在", 404);
        }
        return user;
    }

    private UserProfileVO toProfile(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setHandle(user.getHandle());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setAvatarEmoji(user.getAvatarEmoji());
        vo.setProfileBgTheme(user.getProfileBgTheme() != null ? user.getProfileBgTheme() : 0);
        vo.setCity(user.getCity());
        vo.setBio(user.getBio());
        vo.setGender(user.getGender());
        vo.setMbti(user.getMbti());
        vo.setFrequentArea(user.getFrequentArea());
        vo.setFavoriteDrink(user.getFavoriteDrink());
        vo.setSpotifyConnected(user.getSpotifyConnected() != null && user.getSpotifyConnected() == 1);
        vo.setSpotifyGenres(JsonUtil.parseStringList(user.getSpotifyGenres()));
        if (user.getFirstCheckInAt() != null) {
            vo.setDrinkAgeMonths((int) ChronoUnit.MONTHS.between(
                    user.getFirstCheckInAt().toLocalDate(), LocalDate.now()));
        } else {
            vo.setDrinkAgeMonths(0);
        }
        vo.setStats(computeStats(user.getId()));
        vo.setTonightEnabled(user.getTonightEnabled() != null && user.getTonightEnabled() == 1);
        vo.setTonightSocialStatus(user.getTonightSocialStatus());
        vo.setCreatedAt(user.getCreatedAt());
        try {
            vo.setPrivacySettings(objectMapper.readValue(user.getPrivacySettings(), PrivacySettingsRequest.class));
        } catch (Exception e) {
            vo.setPrivacySettings(new PrivacySettingsRequest());
        }
        try {
            vo.setSocialPreferences(objectMapper.readValue(user.getSocialPreferences(),
                    new TypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            vo.setSocialPreferences(Map.of());
        }
        return vo;
    }
}
