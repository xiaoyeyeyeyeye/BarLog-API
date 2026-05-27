package com.alcohol.controller;

import com.alcohol.common.Result;
import com.alcohol.dto.auth.*;
import com.alcohol.service.AuthService;
import com.alcohol.vo.auth.AuthMethodsVO;
import com.alcohol.vo.auth.LoginVO;
import com.alcohol.vo.auth.OtpSendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "海外登录注册（新加坡/东南亚）：手机 OTP、邮箱、Google、Facebook")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    @GetMapping("/methods")
    @Operation(summary = "支持的登录方式", description = "默认 +65 新加坡，含支持的区号列表")
    public Result<AuthMethodsVO> methods() {
        return Result.success(authService.supportedMethods());
    }

    @PostMapping("/otp/send")
    @Operation(summary = "发送 OTP", description = "SMS 走 AWS SNS（生产）/ mock（开发）；EMAIL 走 AWS SES / mock")
    public Result<OtpSendVO> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        return Result.success(authService.sendOtp(req));
    }

    @PostMapping("/login/otp")
    @Operation(summary = "OTP 登录/注册", description = "验证通过后：老用户登录，新用户自动注册（需 nickname）")
    public Result<LoginVO> loginOtp(@Valid @RequestBody OtpLoginRequest req) {
        return Result.success(authService.loginWithOtp(req));
    }

    @PostMapping("/register/email")
    @Operation(summary = "邮箱注册", description = "需先 send OTP purpose=REGISTER")
    public Result<LoginVO> registerEmail(@Valid @RequestBody EmailRegisterRequest req) {
        return Result.success(authService.registerEmail(req));
    }

    @PostMapping("/login/email")
    @Operation(summary = "邮箱密码登录")
    public Result<LoginVO> loginEmail(@Valid @RequestBody EmailLoginRequest req) {
        return Result.success(authService.loginEmail(req));
    }

    @PostMapping("/oauth/google")
    @Operation(summary = "Google 登录", description = "客户端 Google Sign-In 获取 idToken 后提交")
    public Result<LoginVO> google(@Valid @RequestBody GoogleLoginRequest req) {
        return Result.success(authService.loginGoogle(req));
    }

    @PostMapping("/oauth/facebook")
    @Operation(summary = "Facebook 登录", description = "客户端 Facebook SDK 获取 accessToken 后提交")
    public Result<LoginVO> facebook(@Valid @RequestBody FacebookLoginRequest req) {
        return Result.success(authService.loginFacebook(req));
    }

    @PostMapping("/register/phone")
    @Operation(summary = "手机号密码注册", description = "国际 E.164，默认新加坡 +65")
    public Result<LoginVO> registerPhone(@Valid @RequestBody PhoneRegisterRequest req) {
        return Result.success(authService.registerPhone(req));
    }

    @PostMapping("/login/phone")
    @Operation(summary = "手机号密码登录")
    public Result<LoginVO> loginPhone(@Valid @RequestBody PhoneLoginRequest req) {
        return Result.success(authService.loginPhone(req));
    }

    /** @deprecated 兼容旧接口，请用 {@link #registerPhone(PhoneRegisterRequest)} */
    @PostMapping("/register/legacy-phone")
    @Operation(summary = "用户注册（兼容旧版）", deprecated = true)
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = LoginVO.class)))
    public Result<LoginVO> registerLegacy(@Valid @RequestBody RegisterRequest req) {
        return Result.success(authService.register(req));
    }

    /** @deprecated 兼容旧接口，请用 {@link #loginPhone(PhoneLoginRequest)} */
    @PostMapping("/login/legacy-phone")
    @Operation(summary = "用户登录（兼容旧版）", deprecated = true)
    public Result<LoginVO> loginLegacy(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req));
    }
}
