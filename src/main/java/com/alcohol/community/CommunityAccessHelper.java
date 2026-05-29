package com.alcohol.community;

import com.alcohol.common.BizException;
import com.alcohol.compat.FrontendMapper;
import com.alcohol.context.UserContext;
import com.alcohol.entity.CheckIn;
import com.alcohol.entity.User;
import com.alcohol.mapper.CheckInMapper;
import com.alcohol.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CommunityAccessHelper {

    private final CheckInMapper checkInMapper;
    private final UserMapper userMapper;
    private final FrontendMapper frontendMapper;

    @Value("${alcohol.gallery-default-hours:24}")
    private int galleryDefaultHours;

    public String requireUserId() {
        String userId = UserContext.getUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BizException("Missing bearer token", 401, "AUTH_REQUIRED");
        }
        return userId;
    }

    public User requireUser() {
        User user = userMapper.selectById(requireUserId());
        if (user == null) {
            throw new BizException("User not found", 401);
        }
        return user;
    }

    public LocalDateTime todayStart() {
        return LocalDate.now().atStartOfDay();
    }

    public int resolveRangeHours(String range) {
        if (!StringUtils.hasText(range)) {
            return galleryDefaultHours;
        }
        return switch (range.trim().toLowerCase()) {
            case "12h" -> 12;
            case "24h" -> 24;
            case "7d" -> 24 * 7;
            case "30d" -> 24 * 30;
            default -> galleryDefaultHours;
        };
    }

    public CheckIn findTodayCheckIn(String userId, String city, String barId) {
        LambdaQueryWrapper<CheckIn> qw = new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .ge(CheckIn::getCreatedAt, todayStart())
                .orderByDesc(CheckIn::getCreatedAt)
                .last("LIMIT 1");
        if (StringUtils.hasText(barId)) {
            qw.eq(CheckIn::getBarId, barId);
        } else if (StringUtils.hasText(city)) {
            String normalized = frontendMapper.normalizeCityIn(city);
            qw.and(w -> w.eq(CheckIn::getCity, normalized).or().eq(CheckIn::getCity, city.trim()));
        }
        return checkInMapper.selectOne(qw);
    }

    public boolean hasTodayCityCheckIn(String userId, String city) {
        if (!StringUtils.hasText(city)) {
            return checkInMapper.selectCount(new LambdaQueryWrapper<CheckIn>()
                    .eq(CheckIn::getUserId, userId)
                    .ge(CheckIn::getCreatedAt, todayStart())) > 0;
        }
        String normalized = frontendMapper.normalizeCityIn(city);
        return checkInMapper.selectCount(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .ge(CheckIn::getCreatedAt, todayStart())
                .and(w -> w.eq(CheckIn::getCity, normalized).or().eq(CheckIn::getCity, city.trim()))) > 0;
    }

    public boolean hasTodayBarCheckIn(String userId, String barId) {
        if (!StringUtils.hasText(barId)) {
            return false;
        }
        return checkInMapper.selectCount(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .eq(CheckIn::getBarId, barId)
                .ge(CheckIn::getCreatedAt, todayStart())) > 0;
    }

    public boolean hasTodayCheckIn(String userId) {
        return checkInMapper.selectCount(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .ge(CheckIn::getCreatedAt, todayStart())) > 0;
    }

    public void assertCommunityUnlocked() {
        String userId = requireUserId();
        if (!hasTodayCheckIn(userId)) {
            throw new BizException("Check in today to unlock community",
                    403, "COMMUNITY_CHECKIN_REQUIRED");
        }
    }

    public CheckIn findLatestTodayCheckIn(String userId) {
        return checkInMapper.selectOne(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .ge(CheckIn::getCreatedAt, todayStart())
                .orderByDesc(CheckIn::getCreatedAt)
                .last("LIMIT 1"));
    }

    public void assertFeedAccess(String scope, String city, String barId) {
        assertCommunityUnlocked();
    }

    public CheckIn requireVisiblePost(String checkInId) {
        CheckIn checkIn = checkInMapper.selectById(checkInId);
        if (checkIn == null) {
            throw new BizException("Post not found", 404);
        }
        if (!isPubliclyVisible(checkIn)) {
            throw new BizException("Post not found", 404);
        }
        return checkIn;
    }

    public boolean isPubliclyVisible(CheckIn checkIn) {
        if (checkIn == null) {
            return false;
        }
        String visibility = checkIn.getVisibility();
        if (!"PUBLIC".equals(visibility) && !"TONIGHT_ONLY".equals(visibility)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (checkIn.getExpiresAt() != null && !checkIn.getExpiresAt().isAfter(now)) {
            return false;
        }
        return true;
    }

    public List<CheckIn> latestOpenCheckInsForUser(String userId, int limit) {
        LocalDateTime now = LocalDateTime.now();
        return checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .in(CheckIn::getVisibility, "PUBLIC", "TONIGHT_ONLY")
                .gt(CheckIn::getExpiresAt, now)
                .orderByDesc(CheckIn::getCreatedAt)
                .last("LIMIT " + limit));
    }
}
