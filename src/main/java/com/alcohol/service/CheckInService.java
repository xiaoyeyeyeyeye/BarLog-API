package com.alcohol.service;

import com.alcohol.common.BizException;
import com.alcohol.context.UserContext;
import com.alcohol.converter.CheckInConverter;
import com.alcohol.dto.checkin.CreateCheckInRequest;
import com.alcohol.entity.*;
import com.alcohol.enums.DrinkCategory;
import com.alcohol.enums.SocialStatus;
import com.alcohol.enums.Visibility;
import com.alcohol.mapper.*;
import com.alcohol.util.JsonUtil;
import com.alcohol.vo.PageVO;
import com.alcohol.vo.checkin.CheckInVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 打卡核心业务。
 * <p>
 * 职责：创建/查询打卡、Gallery 公开流、Tonight 附近打卡。<br>
 * 创建打卡副作用：更新图鉴 → 解锁徽章 → 刷新酒精人格 → 记录首次打卡时间。
 * </p>
 *
 * @see com.alcohol.controller.CheckInController
 */
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInMapper checkInMapper;
    private final UserMapper userMapper;
    private final DrinkMapper drinkMapper;
    private final UserDrinkMapper userDrinkMapper;
    private final BadgeMapper badgeMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final PersonaService personaService;
    private final BarMapper barMapper;
    private final CheckInConverter checkInConverter;

    @Value("${alcohol.gallery-default-hours:12}")
    private int galleryDefaultHours;

    @Transactional
    public CheckInVO create(CreateCheckInRequest req) {
        String userId = UserContext.getUserId();
        validateEnums(req.getDrinkCategory(), req.getVisibility(), req.getCardStyle());

        CheckIn checkIn = buildCheckIn(req, userId);
        checkInMapper.insert(checkIn);

        markFirstCheckInIfNeeded(userId);
        upsertUserDrink(userId, checkIn.getDrinkId());
        unlockBadges(userId, checkIn);
        personaService.refreshPersona(userId);

        return checkInConverter.toVO(checkIn, userMapper.selectById(userId));
    }

    public CheckInVO getById(String id) {
        CheckIn checkIn = checkInMapper.selectById(id);
        if (checkIn == null) {
            throw new BizException("打卡不存在", 404);
        }
        assertReadable(checkIn);
        return checkInConverter.toVO(checkIn, userMapper.selectById(checkIn.getUserId()));
    }

    public PageVO<CheckInVO> listMine(int page, int size) {
        String userId = UserContext.getUserId();
        Page<CheckIn> p = checkInMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<CheckIn>()
                        .eq(CheckIn::getUserId, userId)
                        .orderByDesc(CheckIn::getCreatedAt));
        User user = userMapper.selectById(userId);
        List<CheckInVO> records = p.getRecords().stream()
                .map(c -> checkInConverter.toVO(c, user))
                .toList();
        return new PageVO<>(records, p.getTotal(), page, size);
    }

    /** Gallery：仅 PUBLIC / TONIGHT_ONLY 且未过期、在时间窗口内 */
    public PageVO<CheckInVO> gallery(String city, String area, String drinkCategory, String drinkName,
                                     String moodTag, String socialStatus, Integer hours, int page, int size) {
        LocalDateTime now = LocalDateTime.now();
        int h = hours != null ? hours : galleryDefaultHours;
        LocalDateTime since = now.minusHours(h);

        LambdaQueryWrapper<CheckIn> qw = new LambdaQueryWrapper<CheckIn>()
                .in(CheckIn::getVisibility, Visibility.PUBLIC.name(), Visibility.TONIGHT_ONLY.name())
                .gt(CheckIn::getExpiresAt, now)
                .ge(CheckIn::getCreatedAt, since)
                .orderByDesc(CheckIn::getCreatedAt);

        if (StringUtils.hasText(city)) qw.eq(CheckIn::getCity, city);
        if (StringUtils.hasText(area)) qw.eq(CheckIn::getArea, area);
        if (StringUtils.hasText(drinkCategory)) qw.eq(CheckIn::getDrinkCategory, drinkCategory);
        if (StringUtils.hasText(drinkName)) qw.like(CheckIn::getDrinkName, drinkName);
        if (StringUtils.hasText(socialStatus)) qw.eq(CheckIn::getSocialStatus, socialStatus);

        Page<CheckIn> p = checkInMapper.selectPage(new Page<>(page, size), qw);
        Map<String, User> userCache = new HashMap<>();
        List<CheckInVO> records = p.getRecords().stream()
                .filter(c -> moodTag == null || JsonUtil.parseStringList(c.getMoodTags()).contains(moodTag))
                .map(c -> checkInConverter.toVO(c, userCache.computeIfAbsent(c.getUserId(), userMapper::selectById)))
                .toList();
        return new PageVO<>(records, p.getTotal(), page, size);
    }

    /** 已开启 Tonight Mode 的其他用户的有效公开打卡 */
    public List<CheckInVO> tonightNearby(int limit) {
        String userId = UserContext.getUserId();
        LocalDateTime now = LocalDateTime.now();

        List<String> tonightUserIds = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getTonightEnabled, 1)
                        .ne(User::getId, userId)
        ).stream().map(User::getId).toList();

        if (tonightUserIds.isEmpty()) {
            return List.of();
        }

        List<CheckIn> list = checkInMapper.selectList(
                new LambdaQueryWrapper<CheckIn>()
                        .in(CheckIn::getUserId, tonightUserIds)
                        .in(CheckIn::getVisibility, Visibility.PUBLIC.name(), Visibility.TONIGHT_ONLY.name())
                        .gt(CheckIn::getExpiresAt, now)
                        .orderByDesc(CheckIn::getCreatedAt)
                        .last("LIMIT " + Math.min(limit, 50))
        );

        Map<String, User> cache = new HashMap<>();
        return list.stream()
                .map(c -> checkInConverter.toVO(c, cache.computeIfAbsent(c.getUserId(), userMapper::selectById)))
                .collect(Collectors.toList());
    }

    public PageVO<CheckInVO> listByDrink(String drinkId, int page, int size) {
        LocalDateTime now = LocalDateTime.now();
        Page<CheckIn> p = checkInMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<CheckIn>()
                        .eq(CheckIn::getDrinkId, drinkId)
                        .in(CheckIn::getVisibility, Visibility.PUBLIC.name(), Visibility.TONIGHT_ONLY.name())
                        .gt(CheckIn::getExpiresAt, now)
                        .orderByDesc(CheckIn::getCreatedAt));
        Map<String, User> cache = new HashMap<>();
        List<CheckInVO> records = p.getRecords().stream()
                .map(c -> checkInConverter.toVO(c, cache.computeIfAbsent(c.getUserId(), userMapper::selectById)))
                .toList();
        return new PageVO<>(records, p.getTotal(), page, size);
    }

    // ── 私有：构建与权限 ──────────────────────────────────────────

    private CheckIn buildCheckIn(CreateCheckInRequest req, String userId) {
        CheckIn checkIn = new CheckIn();
        checkIn.setUserId(userId);
        checkIn.setPhotoUrl(req.getPhotoUrl());
        checkIn.setDrinkName(req.getDrinkName());
        checkIn.setDrinkCategory(req.getDrinkCategory());
        checkIn.setDrinkId(resolveDrinkId(req.getDrinkId(), req.getDrinkName()));
        checkIn.setBarId(req.getBarId());
        applyLocation(checkIn, req);
        checkIn.setMoodTags(JsonUtil.toJson(req.getMoodTags()));
        checkIn.setFlavorTags(req.getFlavorTags() != null ? JsonUtil.toJson(req.getFlavorTags()) : "[]");
        checkIn.setVibeMumbling(req.getVibeMumbling());
        checkIn.setDiaryText(StringUtils.hasText(req.getDiaryText()) ? req.getDiaryText() : req.getVibeMumbling());
        checkIn.setRating(req.getRating());
        checkIn.setVoiceNoteUrl(req.getVoiceNoteUrl());
        checkIn.setAiCardQuote(req.getAiCardQuote());
        checkIn.setAiCardQuoteSource(req.getAiCardQuoteSource());
        checkIn.setCardStyle(req.getCardStyle());
        checkIn.setCardImageUrl(StringUtils.hasText(req.getCardImageUrl()) ? req.getCardImageUrl() : req.getPhotoUrl());
        checkIn.setVisibility(req.getVisibility());
        checkIn.setSocialStatus(StringUtils.hasText(req.getSocialStatus()) ? req.getSocialStatus() : SocialStatus.NONE.name());
        checkIn.setLatitude(req.getLatitude());
        checkIn.setLongitude(req.getLongitude());
        checkIn.setCreatedAt(LocalDateTime.now());
        if (Visibility.PUBLIC.name().equals(req.getVisibility())
                || Visibility.TONIGHT_ONLY.name().equals(req.getVisibility())) {
            checkIn.setExpiresAt(LocalDateTime.now().plusHours(galleryDefaultHours));
        }
        return checkIn;
    }

    /** 有 barId 时从 POI 补全地点名与城市 */
    private void applyLocation(CheckIn checkIn, CreateCheckInRequest req) {
        if (StringUtils.hasText(req.getBarId()) && !StringUtils.hasText(req.getLocationName())) {
            Bar bar = barMapper.selectById(req.getBarId());
            if (bar != null) {
                checkIn.setLocationName(bar.getName());
                if (!StringUtils.hasText(req.getCity())) checkIn.setCity(bar.getCity());
                if (!StringUtils.hasText(req.getArea())) checkIn.setArea(bar.getArea());
                return;
            }
        }
        checkIn.setLocationName(req.getLocationName());
        checkIn.setCity(req.getCity());
        checkIn.setArea(req.getArea());
    }

    private void assertReadable(CheckIn checkIn) {
        String currentUserId = UserContext.getUserId();
        boolean isOwner = checkIn.getUserId().equals(currentUserId);
        boolean isPublic = Visibility.PUBLIC.name().equals(checkIn.getVisibility())
                || Visibility.TONIGHT_ONLY.name().equals(checkIn.getVisibility());
        if (!isOwner && !isPublic) {
            throw new BizException("无权查看该打卡", 403);
        }
        if (!isOwner && checkIn.getExpiresAt() != null && checkIn.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException("该打卡已过期", 404);
        }
    }

    private void markFirstCheckInIfNeeded(String userId) {
        User user = userMapper.selectById(userId);
        if (user != null && user.getFirstCheckInAt() == null) {
            user.setFirstCheckInAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    private String resolveDrinkId(String drinkId, String drinkName) {
        if (StringUtils.hasText(drinkId)) {
            return drinkId;
        }
        Drink drink = drinkMapper.selectOne(new LambdaQueryWrapper<Drink>()
                .eq(Drink::getName, drinkName)
                .last("LIMIT 1"));
        return drink != null ? drink.getId() : null;
    }

    private void upsertUserDrink(String userId, String drinkId) {
        if (!StringUtils.hasText(drinkId)) return;
        UserDrink existing = userDrinkMapper.selectOne(new LambdaQueryWrapper<UserDrink>()
                .eq(UserDrink::getUserId, userId)
                .eq(UserDrink::getDrinkId, drinkId));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            UserDrink ud = new UserDrink();
            ud.setUserId(userId);
            ud.setDrinkId(drinkId);
            ud.setCheckInCount(1);
            ud.setFirstUnlockedAt(now);
            ud.setLastCheckInAt(now);
            userDrinkMapper.insert(ud);
        } else {
            existing.setCheckInCount(existing.getCheckInCount() + 1);
            existing.setLastCheckInAt(now);
            userDrinkMapper.updateById(existing);
        }
    }

    private void unlockBadges(String userId, CheckIn checkIn) {
        long totalCheckIns = checkInMapper.selectCount(new LambdaQueryWrapper<CheckIn>().eq(CheckIn::getUserId, userId));
        if (totalCheckIns == 1) grantBadge(userId, "FIRST_CHECK_IN");
        if ("Negroni".equalsIgnoreCase(checkIn.getDrinkName())) grantBadge(userId, "FIRST_NEGRONI");
        if (SocialStatus.NONE.name().equals(checkIn.getSocialStatus())) grantBadge(userId, "SOLO_NIGHT");
        if (DrinkCategory.MOCKTAIL.name().equals(checkIn.getDrinkCategory())) grantBadge(userId, "MOCKTAIL_FRIENDLY");

        long distinctLocations = checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                        .eq(CheckIn::getUserId, userId)
                        .isNotNull(CheckIn::getLocationName))
                .stream().map(CheckIn::getLocationName).distinct().count();
        if (distinctLocations >= 10) grantBadge(userId, "TEN_BARS");

        long distinctCities = checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                        .eq(CheckIn::getUserId, userId)
                        .isNotNull(CheckIn::getCity))
                .stream().map(CheckIn::getCity).distinct().count();
        if (distinctCities >= 3) grantBadge(userId, "THREE_CITIES");
    }

    private void grantBadge(String userId, String badgeCode) {
        Badge badge = badgeMapper.selectOne(new LambdaQueryWrapper<Badge>().eq(Badge::getCode, badgeCode));
        if (badge == null) return;
        Long exists = userBadgeMapper.selectCount(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .eq(UserBadge::getBadgeId, badge.getId()));
        if (exists > 0) return;
        UserBadge ub = new UserBadge();
        ub.setUserId(userId);
        ub.setBadgeId(badge.getId());
        ub.setUnlockedAt(LocalDateTime.now());
        userBadgeMapper.insert(ub);
    }

    private void validateEnums(String drinkCategory, String visibility, String cardStyle) {
        try {
            DrinkCategory.valueOf(drinkCategory);
            Visibility.valueOf(visibility);
            com.alcohol.enums.CardStyle.valueOf(cardStyle);
        } catch (IllegalArgumentException e) {
            throw new BizException("无效的枚举参数");
        }
    }
}
