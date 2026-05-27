package com.alcohol.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "邮箱注册")
public class EmailRegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 32)
    private String password;

    @NotBlank
    @Size(max = 32)
    private String nickname;

    @Schema(description = "邮箱 OTP（需先调用 /otp/send purpose=REGISTER）")
    @NotBlank
    private String otpCode;
}
