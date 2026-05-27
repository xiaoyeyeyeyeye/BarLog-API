package com.alcohol.controller;

import com.alcohol.common.Result;
import com.alcohol.common.SecuredApi;
import com.alcohol.context.UserContext;
import com.alcohol.service.PersonaService;
import com.alcohol.vo.persona.PersonaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/persona")
@RequiredArgsConstructor
@SecuredApi
@Tag(name = "酒精人格", description = "Wall 酒精人格 / BarBTI 行为推断结果")
public class PersonaController {

    private final PersonaService personaService;

    @GetMapping("/me")
    @Operation(summary = "我的酒精人格", description = "无记录时自动根据历史打卡计算")
    public Result<PersonaVO> me() {
        return Result.success(personaService.getMyPersona());
    }

    @PostMapping("/me/refresh")
    @Operation(summary = "重新计算酒精人格")
    public Result<PersonaVO> refresh() {
        return Result.success(personaService.refreshPersona(UserContext.getUserId()));
    }
}
