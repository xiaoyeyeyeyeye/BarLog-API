package com.alcohol.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI 酒卡文案请求")
public class AiCardQuoteRequest {

    private String drinkName;
    private String locationName;
    private String diaryText;
    private List<String> moodTags;
    private List<String> flavorTags;
    private Integer rating;
}
