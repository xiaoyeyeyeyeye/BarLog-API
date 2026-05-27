package com.alcohol.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Facebook 登录（客户端 SDK 获取 accessToken 后提交）")
public class FacebookLoginRequest {

    @NotBlank
    private String accessToken;

    @Size(max = 32)
    private String nickname;
}
