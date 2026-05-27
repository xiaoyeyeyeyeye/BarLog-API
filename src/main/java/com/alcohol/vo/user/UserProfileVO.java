package com.alcohol.vo.user;

import com.alcohol.dto.user.PrivacySettingsRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "用户资料")
public class UserProfileVO {

    private String id;
    private String nickname;
    private String handle;
    private String avatarUrl;
    private String avatarEmoji;
    private Integer profileBgTheme;
    private String city;
    private String bio;
    private String gender;
    private String mbti;
    private String frequentArea;
    private String favoriteDrink;
    private Boolean spotifyConnected;
    private List<String> spotifyGenres;
    private Integer drinkAgeMonths;
    private UserStatsVO stats;
    private PrivacySettingsRequest privacySettings;
    private Map<String, Object> socialPreferences;
    private Boolean tonightEnabled;
    private String tonightSocialStatus;
    private LocalDateTime createdAt;
}
