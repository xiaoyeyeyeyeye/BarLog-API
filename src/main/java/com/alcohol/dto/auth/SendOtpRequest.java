package com.alcohol.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "发送 OTP 验证码")
public class SendOtpRequest {

    @Schema(description = "SMS 或 EMAIL", example = "SMS")
    @NotBlank
    private String channel;

    @Schema(description = "用途：LOGIN / REGISTER / BIND_PHONE / BIND_EMAIL / RESET_PASSWORD", example = "LOGIN")
    @NotBlank
    private String purpose;

    @Schema(description = "手机号（SMS）或邮箱（EMAIL）", example = "91234567")
    @NotBlank
    private String target;

    @Schema(description = "国家区号，SMS 时必填，默认 +65（新加坡）", example = "+65")
    private String countryCode;
}
