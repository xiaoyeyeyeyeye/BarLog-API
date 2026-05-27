package com.alcohol.controller;

import com.alcohol.common.Result;
import com.alcohol.common.ResultVoid;
import com.alcohol.common.SecuredApi;
import com.alcohol.service.BarService;
import com.alcohol.vo.bar.BarVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/bars")
@RequiredArgsConstructor
@SecuredApi
@Tag(name = "酒吧", description = "附近酒吧、排行榜、收藏")
public class BarController {

    private final BarService barService;

    @GetMapping("/nearby")
    @Operation(summary = "附近酒吧", description = "按距离排序，可选半径过滤（米）")
    public Result<List<BarVO>> nearby(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radiusM,
            @RequestParam(required = false, defaultValue = "list") String view) {
        return Result.success(barService.nearby(lat, lng, radiusM, view));
    }

    @GetMapping("/ranking")
    @Operation(summary = "酒吧排行榜")
    public Result<List<BarVO>> ranking(
            @RequestParam(required = false) String city,
            @RequestParam(required = false, defaultValue = "week") String period,
            @RequestParam(required = false) String tag) {
        return Result.success(barService.ranking(city, period, tag));
    }

    @GetMapping("/{id}")
    @Operation(summary = "酒吧详情")
    public Result<BarVO> detail(@PathVariable String id) {
        return Result.success(barService.detail(id));
    }

    @PostMapping("/{id}/favorite")
    @Operation(summary = "收藏酒吧")
    public ResultVoid favorite(@PathVariable String id) {
        barService.favorite(id);
        return ResultVoid.success();
    }

    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "取消收藏")
    public ResultVoid unfavorite(@PathVariable String id) {
        barService.unfavorite(id);
        return ResultVoid.success();
    }
}
