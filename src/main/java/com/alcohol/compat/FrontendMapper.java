package com.alcohol.compat;

import com.alcohol.compat.vo.*;
import com.alcohol.entity.Bar;
import com.alcohol.entity.CheckIn;
import com.alcohol.entity.User;
import com.alcohol.util.JsonUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class FrontendMapper {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    public FrontendUserVO toUser(User user, String persona) {
        if (user == null) {
            return null;
        }
        FrontendUserVO vo = new FrontendUserVO();
        vo.setId(user.getId());
        vo.setDisplayName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : "BarLog Guest");
        vo.setEmail(user.getEmail());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setCity(normalizeCityOut(user.getCity()));
        vo.setPersona(persona);
        return vo;
    }

    public FrontendCheckInVO toCheckIn(CheckIn entity, User user, Bar bar) {
        if (entity == null) {
            return null;
        }
        FrontendCheckInVO vo = new FrontendCheckInVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setPhotoUrl(entity.getPhotoUrl());
        vo.setCardImageUrl(entity.getCardImageUrl());
        vo.setDrinkName(entity.getDrinkName());
        vo.setDrinkCategory(toFrontendCategory(entity.getDrinkCategory()));
        vo.setBarId(entity.getBarId());
        vo.setBarName(resolveBarName(entity, bar));
        vo.setCity(normalizeCityOut(entity.getCity()));
        vo.setArea(entity.getArea());
        List<String> moods = JsonUtil.parseStringList(entity.getMoodTags());
        vo.setMoodTags(moods != null ? moods : Collections.emptyList());
        vo.setRating(toFrontendRating(entity.getRating()));
        vo.setVibeMumbling(entity.getVibeMumbling());
        vo.setCardStyle(toFrontendCardStyle(entity.getCardStyle()));
        vo.setVisibility(toFrontendVisibility(entity.getVisibility()));
        vo.setSocialStatus(toFrontendSocialStatus(entity.getSocialStatus()));
        vo.setCreatedAt(formatInstant(entity.getCreatedAt()));
        vo.setExpiresAt(formatInstant(entity.getExpiresAt()));
        return vo;
    }

    public FrontendBarVO toBar(Bar bar, Integer distanceMeters) {
        if (bar == null) {
            return null;
        }
        FrontendBarVO vo = new FrontendBarVO();
        vo.setId(bar.getId());
        vo.setName(bar.getName());
        vo.setCity(normalizeCityOut(bar.getCity()));
        vo.setArea(bar.getArea());
        vo.setAddress(bar.getAddress());
        vo.setRating(toFrontendBarRating(bar.getAvgRating(), bar.getSource()));
        vo.setDistanceMeters(distanceMeters);
        vo.setLat(bar.getLatitude());
        vo.setLng(bar.getLongitude());
        if (StringUtils.hasText(bar.getTypeLabel())) {
            vo.setTags(Arrays.stream(bar.getTypeLabel().split("[,/&]"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList()));
        }
        vo.setOpeningHours(bar.getOpenHours());
        vo.setReviewCount(bar.getReviewCount());
        vo.setSource(StringUtils.hasText(bar.getSource()) ? bar.getSource() : "seed");
        if (StringUtils.hasText(bar.getTypeLabel())) {
            vo.setDescription(bar.getTypeLabel().replace(",", " · "));
        }
        return vo;
    }

    public FrontendGalleryPostVO toGalleryPost(CheckIn checkIn, User user) {
        FrontendGalleryPostVO post = new FrontendGalleryPostVO();
        post.setId(checkIn.getId());
        post.setUserId(checkIn.getUserId());
        post.setAuthorName(user != null && StringUtils.hasText(user.getNickname()) ? user.getNickname() : "BarLog");
        post.setImageUrl(StringUtils.hasText(checkIn.getCardImageUrl()) ? checkIn.getCardImageUrl() : checkIn.getPhotoUrl());
        post.setCaption(checkIn.getVibeMumbling());
        post.setCity(normalizeCityOut(checkIn.getCity()));
        post.setBarName(checkIn.getLocationName());
        post.setLikedCount(0);
        post.setCreatedAt(formatInstant(checkIn.getCreatedAt()));
        return post;
    }

    public String normalizeCityIn(String city) {
        if (!StringUtils.hasText(city)) {
            return city;
        }
        if ("Shanghai".equalsIgnoreCase(city.trim())) {
            return "上海";
        }
        return city.trim();
    }

    public String normalizeCityOut(String city) {
        if (!StringUtils.hasText(city)) {
            return city;
        }
        if ("上海".equals(city.trim())) {
            return "Shanghai";
        }
        return city.trim();
    }

    public String toBackendCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return category;
        }
        return category.trim().toUpperCase(Locale.ROOT);
    }

    public String toBackendCardStyle(String value) {
        if (!StringUtils.hasText(value)) {
            return "RECEIPT";
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "watercolor" -> "WATERCOLOR";
            case "film_ticket" -> "FILM_TICKET";
            case "doodle_glow" -> "DOODLE_GLOW";
            case "passport_stamp" -> "PASSPORT_STAMP";
            default -> "RECEIPT";
        };
    }

    public String toBackendVisibility(String value) {
        if (!StringUtils.hasText(value)) {
            return "PRIVATE";
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "public" -> "PUBLIC";
            case "tonight_only" -> "TONIGHT_ONLY";
            default -> "PRIVATE";
        };
    }

    public String toBackendSocialStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "open_to_chat" -> "CHAT_OK";
            case "looking_for_buddy" -> "FIND_BUDDY";
            case "friends_only" -> "VIEW_ONLY";
            default -> "NONE";
        };
    }

    private String resolveBarName(CheckIn entity, Bar bar) {
        if (bar != null && StringUtils.hasText(bar.getName())) {
            return bar.getName();
        }
        return entity.getLocationName();
    }

    public String toFrontendCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return "other";
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }

    private String toFrontendCardStyle(String value) {
        if (!StringUtils.hasText(value)) {
            return "receipt";
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "WATERCOLOR" -> "watercolor";
            case "FILM_TICKET" -> "film_ticket";
            case "DOODLE_GLOW" -> "doodle_glow";
            case "PASSPORT_STAMP" -> "passport_stamp";
            default -> "receipt";
        };
    }

    private String toFrontendVisibility(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "PUBLIC" -> "public";
            case "TONIGHT_ONLY" -> "tonight_only";
            default -> "private";
        };
    }

    private String toFrontendSocialStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "OPEN_TO_CHAT", "CHAT_OK" -> "open_to_chat";
            case "LOOKING_FOR_BUDDY", "FIND_BUDDY" -> "looking_for_buddy";
            case "FRIENDS_ONLY", "VIEW_ONLY" -> "friends_only";
            case "NOT_SOCIAL", "NONE" -> "not_social";
            default -> "not_social";
        };
    }

    private Double toFrontendRating(Integer rating) {
        if (rating == null) {
            return null;
        }
        return Math.round(rating / 2.0 * 10.0) / 10.0;
    }

    private Double toFrontendBarRating(BigDecimal avgRating, String source) {
        if (avgRating == null) {
            return null;
        }
        double value = avgRating.doubleValue();
        if ("google".equalsIgnoreCase(source)) {
            return Math.round(value * 10.0) / 10.0;
        }
        return Math.round(value / 2.0 * 10.0) / 10.0;
    }

    private String formatInstant(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(ZoneOffset.UTC).format(ISO);
    }
}
