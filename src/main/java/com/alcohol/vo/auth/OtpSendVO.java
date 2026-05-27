package com.alcohol.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "OTP 发送结果")
public class OtpSendVO {

    private String channel;
    private String targetMasked;
    private int expiresInSeconds;
    private int cooldownSeconds;

    @Schema(description = "仅 mock 模式返回，生产环境为 null")
    private String debugCode;
}
