package com.alcohol.controller;

import com.alcohol.common.Result;
import com.alcohol.common.SecuredApi;
import com.alcohol.service.CheckInService;
import com.alcohol.service.DrinkService;
import com.alcohol.vo.PageVO;
import com.alcohol.vo.checkin.CheckInVO;
import com.alcohol.vo.drink.DrinkVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/drinks")
@RequiredArgsConstructor
@SecuredApi
@Tag(name = "酒款", description = "经典鸡尾酒 Cocktail List 与单酒社区")
public class DrinkController {

    private final DrinkService drinkService;
    private final CheckInService checkInService;

    @GetMapping
    @Operation(summary = "经典鸡尾酒列表", description = "含最近 12 小时打卡数与最常见心情")
    public Result<List<DrinkVO>> list() {
        return Result.success(drinkService.listClassics());
    }

    @GetMapping("/{id}")
    @Operation(summary = "酒款详情")
    public Result<DrinkVO> detail(@Parameter(description = "酒款 ID，如 drink-negroni") @PathVariable String id) {
        return Result.success(drinkService.getDetail(id));
    }

    @GetMapping("/{id}/check-ins")
    @Operation(summary = "某款酒下的公开打卡", description = "最近 12 小时内、未过期")
    public Result<PageVO<CheckInVO>> drinkCheckIns(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(checkInService.listByDrink(id, page, size));
    }
}
