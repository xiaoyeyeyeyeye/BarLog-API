package com.alcohol.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 同事 Expo 前端 API 兼容层配置。
 * <p>开发阶段允许无 Bearer Token 时使用 demo 账号，便于本地 Web 预览（登录页当前仅写本地会话）。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "alcohol.compat")
public class CompatProperties {

    /** 无 Token 时是否回落到 demo 用户（仅建议开发环境开启） */
    private boolean allowAnonymous = true;

    /** demo 用户邮箱，不存在则在首次匿名请求时自动创建 */
    private String demoUserEmail = "demo@barlog.app";

    private String demoUserPassword = "password123";

    private String demoUserDisplayName = "Mina Chen";
}
