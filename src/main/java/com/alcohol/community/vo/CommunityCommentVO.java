package com.alcohol.community.vo;

import lombok.Data;

@Data
public class CommunityCommentVO {

    private String id;
    private String userId;
    private String authorName;
    private String avatarUrl;
    private String body;
    private String createdAt;
}
