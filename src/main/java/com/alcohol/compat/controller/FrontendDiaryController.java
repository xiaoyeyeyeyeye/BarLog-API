package com.alcohol.compat.controller;

import com.alcohol.compat.service.FrontendCompatService;
import com.alcohol.compat.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FrontendDiaryController {

    private final FrontendCompatService compatService;

    @GetMapping("/api/diary/summary")
    public FrontendDiarySummaryVO summary(@RequestParam(required = false) String month) {
        return compatService.diarySummary(month);
    }

    @GetMapping("/api/diary/calendar")
    public List<FrontendCalendarDayVO> calendar(@RequestParam(required = false) String month) {
        return compatService.diaryCalendar(month);
    }

    @GetMapping("/api/diary/stats")
    public FrontendDiaryStatsVO stats() {
        return compatService.diaryStats();
    }

    @GetMapping("/api/users/{userId}/checkins")
    public FrontendItemsResponse<FrontendCheckInVO> userCheckIns(@PathVariable String userId) {
        return compatService.userCheckIns(userId);
    }
}
