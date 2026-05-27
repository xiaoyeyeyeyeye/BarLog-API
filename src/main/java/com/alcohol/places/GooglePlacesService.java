package com.alcohol.places;

import com.alcohol.compat.vo.FrontendBarVO;
import com.alcohol.config.GooglePlacesProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 酒吧发现：Google Places 搜索 + 缓存 + 每日请求上限。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GooglePlacesService {

    private final GooglePlacesProperties properties;
    private final GooglePlacesClient client;
    private final PlacesToBarMapper mapper;
    private final PlacesUsageLimiter usageLimiter;

    private final Map<String, ListCacheEntry> listCache = new ConcurrentHashMap<>();
    private final Map<String, DetailCacheEntry> detailCache = new ConcurrentHashMap<>();

    public boolean isAvailable() {
        return properties.isConfigured();
    }

    public List<FrontendBarVO> searchNearby(Double lat, Double lng, String city) {
        if (!isAvailable()) {
            return List.of();
        }
        if (lat != null && lng != null) {
            return cachedList("nearby:" + lat + ":" + lng, () -> searchNearbyInternal(lat, lng));
        }
        String queryCity = StringUtils.hasText(city) ? city : properties.getDefaultCity();
        return cachedList("text:" + queryCity.toLowerCase(), () -> searchTextInternal(queryCity));
    }

    public FrontendBarVO getDetails(String barId, Double userLat, Double userLng) {
        if (!isAvailable()) {
            return null;
        }
        String placeId = mapper.extractPlaceId(barId);
        if (!StringUtils.hasText(placeId)) {
            return null;
        }
        return cachedDetail(barId, () -> fetchDetailsInternal(placeId, userLat, userLng));
    }

    public PlacesUsageSnapshot usageSnapshot() {
        return new PlacesUsageSnapshot(
                usageLimiter.currentDailyCount(),
                usageLimiter.dailyLimit(),
                isAvailable());
    }

    private FrontendBarVO fetchDetailsInternal(String placeId, Double userLat, Double userLng) {
        if (!usageLimiter.tryAcquire("placeDetails:" + placeId)) {
            return null;
        }
        GooglePlace place = client.getPlaceDetails(placeId);
        if (place == null) {
            return null;
        }
        return mapper.toDetailBar(place, userLat, userLng, properties.getDefaultCity());
    }

    private List<FrontendBarVO> searchNearbyInternal(double lat, double lng) {
        if (!usageLimiter.tryAcquire("searchNearby")) {
            return List.of();
        }
        List<GooglePlace> places = client.searchNearby(lat, lng, properties.getSearchRadiusM());
        return toSortedBars(places, lat, lng, null);
    }

    private List<FrontendBarVO> searchTextInternal(String city) {
        if (!usageLimiter.tryAcquire("searchText:" + city)) {
            return List.of();
        }
        List<GooglePlace> places = client.searchText("bars in " + city);
        return toSortedBars(places, null, null, city);
    }

    private List<FrontendBarVO> toSortedBars(List<GooglePlace> places, Double lat, Double lng, String city) {
        return places.stream()
                .map(p -> mapper.toBar(p, lat, lng, city))
                .filter(vo -> vo != null && StringUtils.hasText(vo.getName()))
                .sorted(Comparator.comparingInt(b -> b.getDistanceMeters() != null ? b.getDistanceMeters() : Integer.MAX_VALUE))
                .collect(Collectors.toList());
    }

    private List<FrontendBarVO> cachedList(String key, java.util.function.Supplier<List<FrontendBarVO>> loader) {
        int ttl = properties.getCacheTtlSeconds();
        if (ttl <= 0) {
            return loader.get();
        }
        ListCacheEntry entry = listCache.get(key);
        Instant now = Instant.now();
        if (entry != null && entry.expiresAt.isAfter(now)) {
            return entry.bars;
        }
        List<FrontendBarVO> bars = loader.get();
        listCache.put(key, new ListCacheEntry(bars, now.plusSeconds(ttl)));
        return bars;
    }

    private FrontendBarVO cachedDetail(String barId, java.util.function.Supplier<FrontendBarVO> loader) {
        int ttl = properties.getDetailCacheTtlSeconds();
        if (ttl <= 0) {
            return loader.get();
        }
        DetailCacheEntry entry = detailCache.get(barId);
        Instant now = Instant.now();
        if (entry != null && entry.expiresAt.isAfter(now)) {
            return entry.bar;
        }
        FrontendBarVO bar = loader.get();
        if (bar != null) {
            detailCache.put(barId, new DetailCacheEntry(bar, now.plusSeconds(ttl)));
        }
        return bar;
    }

    private record ListCacheEntry(List<FrontendBarVO> bars, Instant expiresAt) {
    }

    private record DetailCacheEntry(FrontendBarVO bar, Instant expiresAt) {
    }

    public record PlacesUsageSnapshot(int dailyUsed, int dailyLimit, boolean enabled) {
    }
}
