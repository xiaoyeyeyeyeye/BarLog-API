package com.alcohol.dto.checkin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建打卡请求")
public class CreateCheckInRequest {

    @Schema(example = "/uploads/photos/xxx.jpg")
    @NotBlank(message = "请上传酒照")
    private String photoUrl;

    @Schema(example = "Aperol Spritz")
    @NotBlank(message = "酒名不能为空")
    private String drinkName;

    @Schema(example = "COCKTAIL")
    @NotBlank(message = "请选择酒类")
    private String drinkCategory;

    private String drinkId;

    @Schema(description = "关联酒吧 POI")
    private String barId;

    @Schema(example = "The Botanist")
    private String locationName;

    private String city;
    private String area;

    @NotEmpty(message = "请至少选择一个心情标签")
    private List<String> moodTags;

    @Schema(description = "风味标签：酸甜/清爽/微苦等")
    private List<String> flavorTags;

    @Schema(description = "一口余味（PRD）")
    @Size(max = 120)
    private String vibeMumbling;

    @Schema(description = "今晚的日记（Demo）")
    private String diaryText;

    @Schema(description = "评分 1-10")
    @Min(1) @Max(10)
    private Integer rating;

    private String voiceNoteUrl;
    private String aiCardQuote;
    private String aiCardQuoteSource;

    @NotBlank(message = "请选择卡片风格")
    private String cardStyle;

    @NotBlank(message = "请设置可见性")
    private String visibility;

    private String socialStatus;
    private String cardImageUrl;
    private Double latitude;
    private Double longitude;
}
