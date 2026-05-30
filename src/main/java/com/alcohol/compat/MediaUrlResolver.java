package com.alcohol.compat;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MediaUrlResolver {

    public boolean isPlaceholderUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return true;
        }
        String normalized = url.trim().toLowerCase();
        return normalized.contains("barlog.local") || normalized.contains("images.barlog");
    }

    public String resolveCheckInImage(String url, String checkInId) {
        if (isPlaceholderUrl(url)) {
            return sipCardPlaceholderPath(checkInId);
        }
        return url;
    }

    public String resolveAvatarUrl(String url, String userId) {
        if (isPlaceholderUrl(url) || !StringUtils.hasText(url)) {
            return avatarPlaceholderPath(userId);
        }
        return url;
    }

    public String sipCardPlaceholderPath(String checkInId) {
        return "/api/media/sip-card/" + checkInId;
    }

    public String avatarPlaceholderPath(String userId) {
        return "/api/media/avatar/" + userId;
    }
}
