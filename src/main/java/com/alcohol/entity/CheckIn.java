package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("check_ins")
public class CheckIn {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;
    private String photoUrl;
    private String drinkName;
    private String drinkId;
    private String drinkCategory;
    private String barId;
    private String locationName;
    private String city;
    private String area;
    private String moodTags;
    private String flavorTags;
    private String vibeMumbling;
    private String diaryText;
    private Integer rating;
    private String voiceNoteUrl;
    private String aiCardQuote;
    private String aiCardQuoteSource;
    private String cardStyle;
    private String cardImageUrl;
    private String visibility;
    private String socialStatus;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
