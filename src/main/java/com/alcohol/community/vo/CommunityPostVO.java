package com.alcohol.community.vo;

import lombok.Data;

@Data
public class CommunityPostVO {

    private String id;
    private String userId;
    private String authorName;
    private String avatarUrl;
    private String imageUrl;
    private String caption;
    private String city;
    private String barId;
    private String barName;
    private String socialStatus;
    private String visibility;
    private int likedCount;
    private int commentCount;
    private boolean likedByMe;
    private String expiresAt;
    private String createdAt;
}
