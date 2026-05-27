package com.alcohol.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "国际手机号 + 密码注册（兼容旧版，支持新加坡等 E.164）")
public class PhoneRegisterRequest {

    @NotBlank
    private String phone;

    @Schema(example = "+65")
    private String countryCode;

    @NotBlank
    @Size(min = 6, max = 32)
    private String password;

    @NotBlank
    @Size(max = 32)
    private String nickname;
}
