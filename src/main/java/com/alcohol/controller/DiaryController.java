package com.alcohol.controller;

import com.alcohol.common.Result;
import com.alcohol.common.SecuredApi;
import com.alcohol.service.DiaryService;
import com.alcohol.vo.PageVO;
import com.alcohol.vo.checkin.CheckInVO;
import com.alcohol.vo.diary.DiarySummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/diary")
@RequiredArgsConstructor
@SecuredApi
@Tag(name = "日记", description = "日记 Tab 月历汇总与近期打卡")
public class DiaryController {

    private final DiaryService diaryService;

    @GetMapping("/summary")
    @Operation(summary = "日记月历汇总", description = "指定年月的打卡统计与打点日期")
    public Result<DiarySummaryVO> summary(
            @Parameter(description = "年份") @RequestParam int year,
            @Parameter(description = "月份 1-12") @RequestParam int month) {
        return Result.success(diaryService.summary(year, month));
    }

    @GetMapping("/recent")
    @Operation(summary = "近期打卡列表", description = "带评分与相对时间文案")
    public Result<PageVO<CheckInVO>> recent(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(diaryService.recent(page, size));
    }
}
