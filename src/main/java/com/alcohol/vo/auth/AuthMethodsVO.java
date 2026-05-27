package com.alcohol.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "支持的登录方式（按区域）")
public class AuthMethodsVO {

    private String defaultCountryCode;
    private String defaultLocale;
    private List<String> supportedCountryCodes;
    private List<MethodItem> methods;

    @Data
    public static class MethodItem {
        private String code;
        private String name;
        private String description;
    }
}
