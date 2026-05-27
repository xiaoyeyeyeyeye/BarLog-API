package com.alcohol.util;

import com.alcohol.entity.CheckIn;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 打卡列表上的统计计算（不访问数据库，对内存列表聚合）。
 * <p>用于用户 stats、日记月历汇总等场景。</p>
 */
public final class CheckInStatsUtil {

    private CheckInStatsUtil() {
    }

    /** 去过的酒吧数：优先 barId，否则 locationName */
    public static long distinctBars(List<CheckIn> checkIns) {
        return checkIns.stream()
                .map(c -> StringUtils.hasText(c.getBarId()) ? c.getBarId() : c.getLocationName())
                .filter(StringUtils::hasText)
                .distinct()
                .count();
    }

    /** 有照片或卡片图的打卡数（酒卡墙计数） */
    public static long cardCount(List<CheckIn> checkIns) {
        return checkIns.stream()
                .filter(c -> StringUtils.hasText(c.getCardImageUrl()) || StringUtils.hasText(c.getPhotoUrl()))
                .count();
    }

    /** 评分均值，保留一位小数；无评分时返回 null */
    public static Double avgRating(List<CheckIn> checkIns) {
        int[] ratings = checkIns.stream()
                .map(CheckIn::getRating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .toArray();
        if (ratings.length == 0) {
            return null;
        }
        double sum = 0;
        for (int r : ratings) {
            sum += r;
        }
        return Math.round(sum / ratings.length * 10.0) / 10.0;
    }

    /**
     * 连续打卡天数：从今天（UTC）往前数，有打卡记录则 +1。
     */
    public static int currentStreak(List<CheckIn> checkIns) {
        if (checkIns == null || checkIns.isEmpty()) {
            return 0;
        }
        Set<LocalDate> dates = new HashSet<>();
        for (CheckIn checkIn : checkIns) {
            if (checkIn.getCreatedAt() != null) {
                dates.add(checkIn.getCreatedAt().toLocalDate());
            }
        }
        LocalDate cursor = LocalDate.now(ZoneOffset.UTC);
        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /** 库内 1–10 分制均值 → 前端 1–5 星（一位小数） */
    public static Double avgRatingForFrontend(List<CheckIn> checkIns) {
        Double raw = avgRating(checkIns);
        if (raw == null) {
            return null;
        }
        return Math.round(raw / 2.0 * 10.0) / 10.0;
    }
}
