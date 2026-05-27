package com.alcohol.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "邮箱密码登录")
public class EmailLoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 32)
    private String password;
}
