package com.alcohol.compat.controller;

import com.alcohol.compat.dto.FrontendCreateCheckInRequest;
import com.alcohol.compat.service.FrontendCompatService;
import com.alcohol.compat.vo.FrontendCheckInVO;
import com.alcohol.compat.vo.FrontendItemsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkins")
@RequiredArgsConstructor
public class FrontendCheckInController {

    private final FrontendCompatService compatService;

    @GetMapping("/recent")
    public FrontendItemsResponse<FrontendCheckInVO> recent() {
        return compatService.recentCheckIns();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FrontendCheckInVO create(@Valid @RequestBody FrontendCreateCheckInRequest req) {
        return compatService.createCheckIn(req);
    }

    @GetMapping("/{checkinId}")
    public FrontendCheckInVO detail(@PathVariable String checkinId) {
        return compatService.getCheckIn(checkinId);
    }

    @DeleteMapping("/{checkinId}")
    public void delete(@PathVariable String checkinId) {
        compatService.deleteCheckIn(checkinId);
    }
}
