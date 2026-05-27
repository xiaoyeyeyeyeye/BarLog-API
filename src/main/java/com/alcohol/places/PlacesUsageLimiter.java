package com.alcohol.places;

import com.alcohol.config.GooglePlacesProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 进程内 Google Places 请求计数与每日上限（研发防刷；生产建议配合 GCP 预算告警）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlacesUsageLimiter {

    private final GooglePlacesProperties properties;

    private final AtomicInteger dailyCount = new AtomicInteger(0);
    @Getter
    private volatile LocalDate usageDay = LocalDate.now(ZoneOffset.UTC);

    /**
     * @return true 允许发起 Google 请求；false 应降级到本地 seed / 缓存
     */
    public boolean tryAcquire(String operation) {
        int limit = properties.getDailyRequestLimit();
        if (limit <= 0) {
            return true;
        }
        resetIfNewDay();
        int current = dailyCount.get();
        if (current >= limit) {
            log.warn("Google Places daily limit reached ({}/{}), skip {} — falling back",
                    current, limit, operation);
            return false;
        }
        int after = dailyCount.incrementAndGet();
        int warnAt = limit * properties.getWarnThresholdPercent() / 100;
        if (after == warnAt || after == limit) {
            log.warn("Google Places daily usage {}/{} ({}%) after {}",
                    after, limit, (after * 100 / limit), operation);
        }
        return true;
    }

    public int currentDailyCount() {
        resetIfNewDay();
        return dailyCount.get();
    }

    public int dailyLimit() {
        return properties.getDailyRequestLimit();
    }

    private void resetIfNewDay() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (!today.equals(usageDay)) {
            synchronized (this) {
                if (!today.equals(usageDay)) {
                    dailyCount.set(0);
                    usageDay = today;
                    log.info("Google Places daily usage counter reset for {}", today);
                }
            }
        }
    }
}
