package com.alcohol.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Google 登录（客户端 SDK 获取 idToken 后提交）")
public class GoogleLoginRequest {

    @NotBlank
    private String idToken;

    @Size(max = 32)
    private String nickname;
}
