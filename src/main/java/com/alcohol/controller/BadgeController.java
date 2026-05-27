package com.alcohol.controller;

import com.alcohol.common.Result;
import com.alcohol.common.SecuredApi;
import com.alcohol.service.BadgeService;
import com.alcohol.vo.badge.BadgeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
@SecuredApi
@Tag(name = "徽章", description = "徽章定义与我的徽章墙")
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping
    @Operation(summary = "全部徽章定义")
    public Result<List<BadgeVO>> listAll() {
        return Result.success(badgeService.listAll());
    }

    @GetMapping("/me")
    @Operation(summary = "我的徽章", description = "含已解锁与未解锁状态")
    public Result<List<BadgeVO>> mine() {
        return Result.success(badgeService.listMine());
    }
}
