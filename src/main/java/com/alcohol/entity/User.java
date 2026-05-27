package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String phone;
    private String password;
    private String email;
    private Integer emailVerified;
    private Integer phoneVerified;
    private String countryCode;
    private String locale;
    private String primaryAuthProvider;
    private Integer status;
    private String nickname;
    private String avatarUrl;
    private String handle;
    private String avatarEmoji;
    private Integer profileBgTheme;
    private String city;
    private String bio;
    private String gender;
    private String mbti;
    private String frequentArea;
    private String favoriteDrink;
    private Integer spotifyConnected;
    private String spotifyGenres;
    private LocalDateTime firstCheckInAt;
    private String privacySettings;
    private String socialPreferences;
    private Integer tonightEnabled;
    private String tonightSocialStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
