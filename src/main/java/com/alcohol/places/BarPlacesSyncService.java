package com.alcohol.places;

import com.alcohol.entity.Bar;
import com.alcohol.mapper.BarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 将 Google Place 同步到本地 {@code bars} 表，供打卡外键与 checkInCount 统计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BarPlacesSyncService {

    private final GooglePlacesClient googlePlacesClient;
    private final PlacesToBarMapper placesMapper;
    private final BarMapper barMapper;

    /**
     * 打卡前确保 bars 行存在（Google barId 以 gp_ 开头）。
     */
    @Transactional
    public void ensureBarExists(String barId) {
        if (!placesMapper.isGoogleBarId(barId)) {
            return;
        }
        if (barMapper.selectById(barId) != null) {
            return;
        }
        String placeId = placesMapper.extractPlaceId(barId);
        if (!StringUtils.hasText(placeId)) {
            return;
        }
        GooglePlace place = googlePlacesClient.getPlaceDetails(placeId);
        if (place == null) {
            log.warn("Cannot sync Google bar {}, details not found", barId);
            return;
        }
        upsertFromGooglePlace(place, barId);
    }

    @Transactional
    public Bar upsertFromGooglePlace(GooglePlace place, String barId) {
        String placeId = place.placeId();
        String id = StringUtils.hasText(barId) ? barId : placesMapper.toBarId(placeId);

        Bar existing = barMapper.selectById(id);
        Bar bar = existing != null ? existing : new Bar();
        bar.setId(id);
        bar.setGooglePlaceId(placeId);
        bar.setSource("google");
        bar.setName(place.displayName());
        bar.setAddress(place.formattedAddress());
        bar.setCity(place.locality());
        bar.setArea(place.sublocality());
        bar.setLatitude(place.latitude());
        bar.setLongitude(place.longitude());
        if (place.rating() != null) {
            bar.setAvgRating(BigDecimal.valueOf(place.rating()).setScale(1, java.math.RoundingMode.HALF_UP));
        }
        if (place.userRatingCount() != null) {
            bar.setReviewCount(place.userRatingCount());
        }
        bar.setOpenHours(place.openingHoursSummary());
        if (place.primaryType() != null) {
            bar.setTypeLabel(place.primaryType());
        }
        bar.setIsActive(1);
        bar.setSyncedAt(LocalDateTime.now());
        if (existing == null) {
            bar.setCreatedAt(LocalDateTime.now());
            barMapper.insert(bar);
        } else {
            barMapper.updateById(bar);
        }
        return bar;
    }
}
