package com.alcohol.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录/注册响应")
public class LoginVO {

    @Schema(description = "用户 ID")
    private String userId;

    @Schema(example = "深夜的 Ellie")
    private String nickname;

    @Schema(description = "JWT，后续请求放 Header: Authorization: Bearer {token}")
    private String token;

    @Schema(description = "是否新注册用户")
    private Boolean newUser;

    @Schema(description = "主登录方式：PHONE/EMAIL/GOOGLE/FACEBOOK")
    private String authProvider;
}
