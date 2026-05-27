package com.alcohol.compat.vo;

import lombok.Data;

@Data
public class FrontendGalleryPostVO {

    private String id;
    private String userId;
    private String authorName;
    private String imageUrl;
    private String caption;
    private String city;
    private String barName;
    private int likedCount;
    private String createdAt;
}
