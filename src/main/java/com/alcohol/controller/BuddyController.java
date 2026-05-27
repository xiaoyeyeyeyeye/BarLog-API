package com.alcohol.controller;

import com.alcohol.common.Result;
import com.alcohol.common.SecuredApi;
import com.alcohol.service.BuddyService;
import com.alcohol.vo.buddy.BuddyMatchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/buddy")
@RequiredArgsConstructor
@SecuredApi
@Tag(name = "社交", description = "摇一摇找搭子")
public class BuddyController {

    private final BuddyService buddyService;

    @PostMapping("/shake")
    @Operation(summary = "摇一摇找搭子", description = "匹配 Tonight 用户或随机推荐")
    public Result<BuddyMatchVO> shake() {
        return Result.success(buddyService.shake());
    }
}
