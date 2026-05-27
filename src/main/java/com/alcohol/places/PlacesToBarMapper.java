package com.alcohol.places;

import com.alcohol.compat.vo.FrontendBarVO;
import com.alcohol.util.GeoUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Google Place → 前端 {@link FrontendBarVO} 映射。
 */
@Component
public class PlacesToBarMapper {

    public static final String BAR_ID_PREFIX = "gp_";

    public FrontendBarVO toBar(GooglePlace place, Double userLat, Double userLng, String fallbackCity) {
        return toBar(place, userLat, userLng, fallbackCity, false);
    }

    public FrontendBarVO toDetailBar(GooglePlace place, Double userLat, Double userLng, String fallbackCity) {
        return toBar(place, userLat, userLng, fallbackCity, true);
    }

    private FrontendBarVO toBar(GooglePlace place, Double userLat, Double userLng, String fallbackCity, boolean detail) {
        if (place == null) {
            return null;
        }
        FrontendBarVO vo = new FrontendBarVO();
        vo.setId(toBarId(place.placeId()));
        vo.setName(place.displayName());
        vo.setAddress(place.formattedAddress());
        vo.setCity(resolveCity(place, fallbackCity));
        vo.setArea(place.sublocality());
        vo.setLat(place.latitude());
        vo.setLng(place.longitude());
        vo.setRating(roundRating(place.rating()));
        vo.setTags(toTags(place));
        vo.setSource("google");
        if (detail) {
            vo.setOpeningHours(place.fullOpeningHours());
            vo.setDescription(place.shortDescription());
            vo.setReviewCount(place.userRatingCount());
            vo.setWebsiteUrl(place.websiteUri());
            vo.setGoogleMapsUrl(place.googleMapsUri());
        } else {
            vo.setOpeningHours(place.openingHoursSummary());
        }
        if (userLat != null && userLng != null && place.latitude() != null && place.longitude() != null) {
            vo.setDistanceMeters((int) GeoUtil.distanceMeters(userLat, userLng, place.latitude(), place.longitude()));
        }
        return vo;
    }

    public String toBarId(String placeId) {
        if (!StringUtils.hasText(placeId)) {
            return null;
        }
        return BAR_ID_PREFIX + placeId;
    }

    public String extractPlaceId(String barId) {
        if (!StringUtils.hasText(barId)) {
            return null;
        }
        if (barId.startsWith(BAR_ID_PREFIX)) {
            return barId.substring(BAR_ID_PREFIX.length());
        }
        return null;
    }

    public boolean isGoogleBarId(String barId) {
        return StringUtils.hasText(barId) && barId.startsWith(BAR_ID_PREFIX);
    }

    private String resolveCity(GooglePlace place, String fallbackCity) {
        if (StringUtils.hasText(place.locality())) {
            return place.locality();
        }
        String address = place.formattedAddress();
        if (StringUtils.hasText(address) && StringUtils.hasText(fallbackCity)
                && address.toLowerCase(Locale.ROOT).contains(fallbackCity.toLowerCase(Locale.ROOT))) {
            return fallbackCity;
        }
        return StringUtils.hasText(fallbackCity) ? fallbackCity : place.locality();
    }

    private List<String> toTags(GooglePlace place) {
        Set<String> tags = new LinkedHashSet<>();
        if (StringUtils.hasText(place.primaryType())) {
            tags.add(place.primaryType().toLowerCase(Locale.ROOT).replace(' ', '_'));
        }
        for (String type : place.types()) {
            if (type.startsWith("bar") || type.contains("night") || type.contains("pub")
                    || type.contains("cocktail") || type.contains("wine")) {
                tags.add(type.toLowerCase(Locale.ROOT).replace(' ', '_'));
            }
        }
        if (tags.isEmpty()) {
            tags.add("bar");
        }
        return new ArrayList<>(tags);
    }

    private Double roundRating(Double rating) {
        if (rating == null) {
            return null;
        }
        return BigDecimal.valueOf(rating).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
