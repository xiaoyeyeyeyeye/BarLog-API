package com.alcohol.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI 喝什么推荐请求")
public class AiDrinkRecommendRequest {

    @Schema(example = "狂欢")
    private String mood;

    private Boolean spotifyConnected;
    private List<String> spotifyGenres;

    @Schema(description = "DIY 或 BAR", example = "DIY")
    private String resultTab;
}
