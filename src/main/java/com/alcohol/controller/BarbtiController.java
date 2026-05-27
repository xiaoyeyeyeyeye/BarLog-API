package com.alcohol.controller;

import com.alcohol.common.Result;
import com.alcohol.common.SecuredApi;
import com.alcohol.dto.barbti.BarbtiSubmitRequest;
import com.alcohol.service.BarbtiService;
import com.alcohol.vo.barbti.BarbtiQuestionVO;
import com.alcohol.vo.barbti.BarbtiVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/barbti")
@RequiredArgsConstructor
@SecuredApi
@Tag(name = "BarBTI", description = "酒吧人格问卷测试")
public class BarbtiController {

    private final BarbtiService barbtiService;

    @GetMapping("/questions")
    @Operation(summary = "获取 20 道 BarBTI 题目")
    public Result<BarbtiQuestionVO.QuestionList> questions() {
        return Result.success(barbtiService.listQuestions());
    }

    @GetMapping("/me")
    @Operation(summary = "我的 BarBTI 结果")
    public Result<BarbtiVO> mine() {
        return Result.success(barbtiService.getMine());
    }

    @PostMapping("/submit")
    @Operation(summary = "提交 BarBTI 答案", description = "answers 为 20 个选项索引（0-3）")
    public Result<BarbtiVO> submit(@Valid @RequestBody BarbtiSubmitRequest req) {
        return Result.success(barbtiService.submit(req));
    }
}
