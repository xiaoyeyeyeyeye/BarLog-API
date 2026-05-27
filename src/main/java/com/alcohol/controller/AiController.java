package com.alcohol.controller;

import com.alcohol.common.Result;
import com.alcohol.common.SecuredApi;
import com.alcohol.dto.ai.AiCardQuoteRequest;
import com.alcohol.dto.ai.AiDrinkRecommendRequest;
import com.alcohol.service.AiService;
import com.alcohol.vo.ai.AiCardQuoteVO;
import com.alcohol.vo.ai.AiDrinkRecommendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@SecuredApi
@Tag(name = "AI", description = "AI 酒卡文案与喝什么推荐（MVP Mock）")
public class AiController {

    private final AiService aiService;

    @PostMapping("/card-quote")
    @Operation(summary = "生成 AI 酒卡文案")
    public Result<AiCardQuoteVO> cardQuote(@RequestBody AiCardQuoteRequest req) {
        return Result.success(aiService.cardQuote(req));
    }

    @PostMapping("/drink-recommend")
    @Operation(summary = "今晚喝什么推荐", description = "返回 DIY 配方与酒吧推荐")
    public Result<AiDrinkRecommendVO> drinkRecommend(@RequestBody AiDrinkRecommendRequest req) {
        return Result.success(aiService.drinkRecommend(req));
    }
}
