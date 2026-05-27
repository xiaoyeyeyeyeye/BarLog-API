package com.alcohol.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI 喝什么推荐")
public class AiDrinkRecommendVO {

    private DiyRecommend diy;
    private BarRecommend bar;

    @Data
    public static class DiyRecommend {
        private String nameZh;
        private String nameEn;
        private String reason;
        private String recipe;
    }

    @Data
    public static class BarRecommend {
        private String barId;
        private String barName;
        private Integer distanceM;
        private String drinkName;
        private String priceLabel;
        private String tag;
        private String reason;
    }
}
