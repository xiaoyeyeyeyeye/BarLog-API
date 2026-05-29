package com.alcohol.compat.controller;

import com.alcohol.compat.service.FrontendCompatService;
import com.alcohol.compat.vo.FrontendBarVO;
import com.alcohol.compat.vo.FrontendCheckInVO;
import com.alcohol.compat.vo.FrontendItemsResponse;
import com.alcohol.compat.vo.FrontendNearbyBarsResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bars")
@RequiredArgsConstructor
public class FrontendBarController {

    private final FrontendCompatService compatService;

    @GetMapping("/nearby")
    public FrontendNearbyBarsResponseVO nearby(@RequestParam(required = false) String city,
                                               @RequestParam(required = false) Double lat,
                                               @RequestParam(required = false) Double lng,
                                               @RequestParam(required = false) Integer radiusMeters) {
        return compatService.nearbyBars(city, lat, lng, radiusMeters);
    }

    @GetMapping("/rankings")
    public List<FrontendBarVO> rankings(@RequestParam(required = false) String city,
                                        @RequestParam(required = false) Double lat,
                                        @RequestParam(required = false) Double lng) {
        return compatService.barRankings(city, lat, lng);
    }

    @GetMapping("/{barId}")
    public FrontendBarVO detail(@PathVariable String barId,
                                @RequestParam(required = false) Double lat,
                                @RequestParam(required = false) Double lng) {
        return compatService.barDetail(barId, lat, lng);
    }

    @GetMapping("/{barId}/checkins")
    public FrontendItemsResponse<FrontendCheckInVO> checkins(@PathVariable String barId) {
        return compatService.barCheckIns(barId);
    }
}
