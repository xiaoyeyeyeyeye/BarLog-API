package com.alcohol.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 相对时间文案：今天 / 昨晚 / 5月18日（日记列表用）。
 */
public final class TimeLabelUtil {

    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("M月d日");

    private TimeLabelUtil() {
    }

    public static String relativeLabel(LocalDateTime time) {
        if (time == null) return "";
        LocalDate today = LocalDate.now();
        LocalDate d = time.toLocalDate();
        long days = ChronoUnit.DAYS.between(d, today);
        if (days == 0) return "今天";
        if (days == 1) return "昨晚";
        if (days == 2) return "前天";
        return MD.format(d);
    }
}
