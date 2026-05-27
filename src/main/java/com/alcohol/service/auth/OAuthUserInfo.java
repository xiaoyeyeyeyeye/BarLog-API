package com.alcohol.service.auth;

import lombok.Data;

@Data
public class OAuthUserInfo {
    private String providerUserId;
    private String email;
    private String name;
    private String avatarUrl;
    private String rawProfile;
}
