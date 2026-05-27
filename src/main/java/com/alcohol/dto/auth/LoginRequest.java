package com.alcohol.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @Schema(example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @Schema(example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}
