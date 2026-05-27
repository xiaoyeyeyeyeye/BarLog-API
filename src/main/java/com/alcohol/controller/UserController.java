package com.alcohol.controller;

import com.alcohol.common.Result;
import com.alcohol.common.SecuredApi;
import com.alcohol.dto.user.PrivacySettingsRequest;
import com.alcohol.dto.user.TonightModeRequest;
import com.alcohol.dto.user.UpdateProfileRequest;
import com.alcohol.service.CollectionService;
import com.alcohol.service.UserService;
import com.alcohol.vo.drink.UserDrinkVO;
import com.alcohol.vo.user.UserProfileVO;
import com.alcohol.vo.user.UserStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecuredApi
@Tag(name = "用户", description = "个人资料、隐私设置、Tonight Mode、酒单图鉴")
public class UserController {

    private final UserService userService;
    private final CollectionService collectionService;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户资料")
    @ApiResponse(responseCode = "401", ref = "Unauthorized")
    public Result<UserProfileVO> me() {
        return Result.success(userService.getMe());
    }

    @PutMapping("/me")
    @Operation(summary = "更新个人资料", description = "字段可选，传则更新（含 handle/emoji/MBTI/Spotify 等 P1 字段）")
    public Result<UserProfileVO> updateMe(@RequestBody UpdateProfileRequest req) {
        return Result.success(userService.updateProfile(req));
    }

    @GetMapping("/me/stats")
    @Operation(summary = "我的统计数据", description = "打卡数、去过酒吧数、酒卡数、均分")
    public Result<UserStatsVO> myStats() {
        return Result.success(userService.getStats());
    }

    @PutMapping("/me/privacy")
    @Operation(summary = "更新隐私设置")
    public Result<UserProfileVO> updatePrivacy(@Valid @RequestBody PrivacySettingsRequest req) {
        return Result.success(userService.updatePrivacy(req));
    }

    @PutMapping("/me/tonight")
    @Operation(summary = "开启/关闭 Tonight Mode")
    public Result<UserProfileVO> updateTonight(@Valid @RequestBody TonightModeRequest req) {
        return Result.success(userService.updateTonightMode(req));
    }

    @GetMapping("/me/collection")
    @Operation(summary = "我的酒单图鉴", description = "已点亮酒款列表")
    public Result<List<UserDrinkVO>> myCollection() {
        return Result.success(collectionService.listMyCollection());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取公开用户资料")
    public Result<UserProfileVO> publicProfile(
            @Parameter(description = "用户 ID") @PathVariable String userId) {
        return Result.success(userService.getPublicProfile(userId));
    }
}
