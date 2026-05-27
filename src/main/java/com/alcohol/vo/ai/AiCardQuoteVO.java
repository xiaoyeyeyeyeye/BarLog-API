package com.alcohol.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI 酒卡文案")
public class AiCardQuoteVO {

    private String quote;
    private String source;
}
