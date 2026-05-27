package com.alcohol.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "国际手机号 + 密码登录")
public class PhoneLoginRequest {

    @NotBlank
    private String phone;

    @Schema(example = "+65")
    private String countryCode;

    @NotBlank
    @Size(min = 6, max = 32)
    private String password;
}
