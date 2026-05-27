package com.alcohol.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Google Places API (New) 配置。API Key 仅通过环境变量注入，勿写入 Git。
 */
@Data
@Component
@ConfigurationProperties(prefix = "alcohol.google.places")
public class GooglePlacesProperties {

    /** 是否调用 Google（false 时酒吧接口回落本地 seed 数据） */
    private boolean enabled = false;

    private String apiKey = "";

    /** 无 city/lat/lng 时的默认城市（英文，如 Singapore） */
    private String defaultCity = "Singapore";

    /** Nearby Search 半径（米） */
    private int searchRadiusM = 3000;

    /** 单次搜索最大结果数 */
    private int maxResultCount = 20;

    /** 列表结果内存缓存 TTL（秒），0 表示不缓存 */
    private int cacheTtlSeconds = 300;

    /** 详情结果内存缓存 TTL（秒），默认 24h，减少重复 Details 调用 */
    private int detailCacheTtlSeconds = 86400;

    /**
     * 每日 Google API 请求上限（搜索 + 详情合计），0 表示不限制。
     * 研发环境建议 500–2000，防止 Expo 热重载刷爆配额。
     */
    private int dailyRequestLimit = 1000;

    /** 达到每日上限的百分之多少时打 WARN 日志（如 80） */
    private int warnThresholdPercent = 80;

    private String baseUrl = "https://places.googleapis.com/v1";

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
