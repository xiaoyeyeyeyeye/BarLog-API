package com.alcohol.vo.checkin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "打卡详情")
public class CheckInVO {

    private String id;
    private String userId;
    private String userNickname;
    private String userAvatarUrl;
    private String photoUrl;
    private String drinkName;
    private String drinkId;
    private String drinkCategory;
    private String barId;
    private String locationName;
    private String city;
    private String area;
    private List<String> moodTags;
    private List<String> flavorTags;
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
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @Schema(description = "相对时间文案：昨晚 / 5月18日")
    private String timeLabel;
}
