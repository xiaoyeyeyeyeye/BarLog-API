package com.alcohol.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "alcohol.auth")
public class AuthProperties {

    /** 默认国家区号（新加坡） */
    private String defaultCountryCode = "+65";

    /** 默认语言区域 */
    private String defaultLocale = "en-SG";

    private Otp otp = new Otp();
    private Sms sms = new Sms();
    private Email email = new Email();
    private OAuth oauth = new OAuth();

    @Data
    public static class Otp {
        private int length = 6;
        private int ttlMinutes = 5;
        private int maxAttempts = 5;
        private int cooldownSeconds = 60;
        private int dailyLimitPerTarget = 10;
    }

    @Data
    public static class Sms {
        /** mock | aws */
        private String provider = "mock";
    }

    @Data
    public static class Email {
        /** mock | aws */
        private String provider = "mock";
        private String fromAddress = "noreply@barlog.app";
    }

    @Data
    public static class OAuth {
        private Google google = new Google();
        private Facebook facebook = new Facebook();

        @Data
        public static class Google {
            private String clientId = "";
        }

        @Data
        public static class Facebook {
            private String appId = "";
            private String appSecret = "";
        }
    }
}
