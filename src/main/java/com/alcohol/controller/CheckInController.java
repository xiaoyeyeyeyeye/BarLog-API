package com.alcohol.controller;

import com.alcohol.common.Result;
import com.alcohol.common.SecuredApi;
import com.alcohol.dto.checkin.CreateCheckInRequest;
import com.alcohol.service.CheckInService;
import com.alcohol.vo.PageVO;
import com.alcohol.vo.checkin.CheckInVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecuredApi
@Tag(name = "打卡", description = "Check-in 创建与查询、Gallery 广场、Tonight 附近")
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping("/api/check-ins")
    @Operation(summary = "创建打卡", description = """
            创建一条打卡记录。公开打卡（PUBLIC / TONIGHT_ONLY）默认 12 小时后从 Gallery 过期。
            副作用：更新酒单图鉴、尝试解锁徽章、刷新酒精人格。
            """)
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    public Result<CheckInVO> create(@Valid @RequestBody CreateCheckInRequest req) {
        return Result.success(checkInService.create(req));
    }

    @GetMapping("/api/check-ins/{id}")
    @Operation(summary = "打卡详情", description = "本人可看全部；他人仅可看未过期的公开打卡")
    public Result<CheckInVO> detail(
            @Parameter(description = "打卡 ID") @PathVariable String id) {
        return Result.success(checkInService.getById(id));
    }

    @GetMapping("/api/check-ins/me")
    @Operation(summary = "我的打卡列表", description = "个人打卡墙 / 日记 Tab 数据源")
    public Result<PageVO<CheckInVO>> mine(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.success(checkInService.listMine(page, size));
    }

    @GetMapping("/api/gallery")
    @Operation(summary = "Gallery 实时打卡流", description = "仅返回 PUBLIC / TONIGHT_ONLY 且未过期的打卡")
    public Result<PageVO<CheckInVO>> gallery(
            @Parameter(description = "城市") @RequestParam(required = false) String city,
            @Parameter(description = "区域") @RequestParam(required = false) String area,
            @Parameter(description = "酒类：COCKTAIL/BEER/…") @RequestParam(required = false) String drinkCategory,
            @Parameter(description = "酒名模糊匹配") @RequestParam(required = false) String drinkName,
            @Parameter(description = "包含该心情标签") @RequestParam(required = false) String moodTag,
            @Parameter(description = "社交状态：NONE/CHAT_OK/…") @RequestParam(required = false) String socialStatus,
            @Parameter(description = "时间窗口（小时），默认 12") @RequestParam(required = false) Integer hours,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(checkInService.gallery(city, area, drinkCategory, drinkName,
                moodTag, socialStatus, hours, page, size));
    }

    @GetMapping("/api/tonight/nearby")
    @Operation(summary = "Tonight 附近用户打卡", description = "返回已开启 Tonight Mode 的其他用户有效打卡")
    public Result<List<CheckInVO>> tonightNearby(
            @Parameter(description = "最大条数，上限 50") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(checkInService.tonightNearby(limit));
    }
}
