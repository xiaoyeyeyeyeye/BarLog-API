package com.alcohol.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "OTP 登录/注册（验证通过后自动注册新用户）")
public class OtpLoginRequest {

    @NotBlank
    private String channel;

    @NotBlank
    private String purpose;

    @NotBlank
    private String target;

    private String countryCode;

    @NotBlank
    @Size(min = 4, max = 8)
    private String code;

    @Schema(description = "新用户注册时必填昵称")
    @Size(max = 32)
    private String nickname;
}
