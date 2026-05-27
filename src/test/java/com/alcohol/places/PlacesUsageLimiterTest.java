package com.alcohol.places;

import com.alcohol.config.GooglePlacesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacesUsageLimiterTest {

    private GooglePlacesProperties properties;
    private PlacesUsageLimiter limiter;

    @BeforeEach
    void setUp() {
        properties = new GooglePlacesProperties();
        properties.setDailyRequestLimit(2);
        properties.setWarnThresholdPercent(50);
        limiter = new PlacesUsageLimiter(properties);
    }

    @Test
    void blocksAfterDailyLimit() {
        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("b"));
        assertFalse(limiter.tryAcquire("c"));
        assertEquals(2, limiter.currentDailyCount());
    }

    @Test
    void unlimitedWhenLimitZero() {
        properties.setDailyRequestLimit(0);
        limiter = new PlacesUsageLimiter(properties);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("x"));
        }
    }
}
