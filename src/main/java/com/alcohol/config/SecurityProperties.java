package com.alcohol.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alcohol.security")
public class SecurityProperties {

    private RateLimit rateLimit = new RateLimit();
    private Password password = new Password();

    @Data
    public static class RateLimit {
        /** 是否启用登录/注册限流 */
        private boolean enabled = true;
        /** 同一 IP 每分钟最多多少次 auth 请求（login/register/otp 等） */
        private int maxRequestsPerMinutePerIp = 30;
        /** 同一 IP 15 分钟内最多多少次登录失败 */
        private int maxFailedLoginsPerIp = 10;
        /** 登录失败计数窗口（分钟） */
        private int failedLoginWindowMinutes = 15;
        /** 同一 IP 1 小时内最多注册次数 */
        private int maxRegistrationsPerIpPerHour = 5;
    }

    @Data
    public static class Password {
        /** 注册时强制最小长度（测试/生产建议开启） */
        private boolean enforcePolicy = true;
        private int minLength = 8;
        private boolean requireLetter = true;
        private boolean requireDigit = true;
    }
}
