package com.alcohol.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "更新个人资料")
public class UpdateProfileRequest {

    private String nickname;
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
}
