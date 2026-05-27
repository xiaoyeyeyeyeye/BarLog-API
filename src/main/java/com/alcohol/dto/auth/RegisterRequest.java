package com.alcohol.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @Schema(description = "手机号（支持国际 E.164 或本地号 + countryCode）", example = "91234567")
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @Schema(example = "+65")
    private String countryCode;

    @Schema(description = "密码 6-32 位", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;

    @Schema(description = "昵称", example = "深夜的 Ellie")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称最多 32 字")
    private String nickname;
}
