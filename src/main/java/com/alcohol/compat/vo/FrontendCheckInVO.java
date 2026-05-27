package com.alcohol.compat.vo;

import lombok.Data;

import java.util.List;

@Data
public class FrontendCheckInVO {

    private String id;
    private String userId;
    private String photoUrl;
    private String cardImageUrl;
    private String drinkName;
    private String drinkCategory;
    private String barId;
    private String barName;
    private String city;
    private String area;
    private List<String> moodTags;
    private Double rating;
    private String vibeMumbling;
    private String cardStyle;
    private String visibility;
    private String socialStatus;
    private String createdAt;
    private String expiresAt;
}
