package com.alcohol.compat.controller;

import com.alcohol.compat.dto.*;
import com.alcohol.compat.service.FrontendCompatService;
import com.alcohol.compat.vo.FrontendAuthResponse;
import com.alcohol.compat.vo.FrontendUserVO;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class FrontendAuthController {

    private final FrontendCompatService compatService;

    @PostMapping("/login")
    public FrontendAuthResponse login(@Valid @RequestBody FrontendLoginRequest req) {
        return compatService.login(req);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public FrontendAuthResponse register(@Valid @RequestBody FrontendRegisterRequest req) {
        return compatService.register(req);
    }

    @PostMapping("/logout")
    public void logout() {
    }

    @PostMapping("/refresh")
    public FrontendAuthResponse refresh(@RequestBody(required = false) FrontendRefreshRequest req) {
        return compatService.refresh(req != null ? req : new FrontendRefreshRequest());
    }

    @GetMapping("/me")
    public FrontendUserVO me() {
        return compatService.me();
    }
}
