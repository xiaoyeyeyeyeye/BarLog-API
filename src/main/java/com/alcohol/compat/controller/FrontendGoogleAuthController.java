package com.alcohol.compat.controller;

import com.alcohol.compat.dto.FrontendGoogleAuthCompleteRequest;
import com.alcohol.compat.dto.FrontendGoogleAuthStartRequest;
import com.alcohol.compat.service.GoogleWebOAuthService;
import com.alcohol.compat.vo.FrontendAuthResponse;
import com.alcohol.compat.vo.FrontendGoogleAuthStartResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
@SecurityRequirements
public class FrontendGoogleAuthController {

    private final GoogleWebOAuthService googleWebOAuthService;

    @PostMapping("/start")
    public FrontendGoogleAuthStartResponse start(@Valid @RequestBody FrontendGoogleAuthStartRequest req) {
        return googleWebOAuthService.start(req.getRedirectUri(), req.getMode());
    }

    @GetMapping("/callback")
    public void callback(@RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         HttpServletResponse response) throws IOException {
        googleWebOAuthService.handleCallback(code, state, response);
    }

    @PostMapping("/complete")
    public FrontendAuthResponse complete(@Valid @RequestBody FrontendGoogleAuthCompleteRequest req) {
        return googleWebOAuthService.complete(req.getAccessToken(), req.getRefreshToken());
    }
}
