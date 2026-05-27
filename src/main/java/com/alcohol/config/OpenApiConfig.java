package com.alcohol.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "BearerAuth";

    @Bean
    public OpenAPI alcoholOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BarLog / 余味 后端 API")
                        .description("""
                                酒吧打卡 App 后端接口，供前端 / Demo 联调使用。

                                ## 通用约定
                                - 响应体统一为 `{ code, msg, result }`，**code=1** 表示成功
                                - 除 **认证** 模块外，请求头需携带 `Authorization: Bearer <token>`
                                - 时间格式：`yyyy-MM-dd HH:mm:ss`（GMT+8）
                                - 上传接口使用 `multipart/form-data`，字段名 `file`

                                ## 枚举
                                - **drinkCategory**: COCKTAIL, BEER, WINE, WHISKY, SAKE, MOCKTAIL, OTHER
                                - **cardStyle**: RECEIPT, FILM_TICKET, DOODLE_GLOW
                                - **visibility**: PRIVATE, PUBLIC, TONIGHT_ONLY
                                - **socialStatus**: NONE, CHAT_OK, FIND_BUDDY, VIEW_ONLY

                                ## 跨域
                                后端已配置 CORS；生产环境建议 Nginx 同域反代 `/api`。
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("BarLog Team").email("dev@example.com"))
                        .license(new License().name("Internal").url("https://example.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8090").description("本地开发"),
                        new Server().url("https://api.example.com").description("生产环境（替换为实际域名）")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录/注册返回的 token，格式：Bearer {token}"))
                        .addResponses("Unauthorized", new ApiResponse()
                                .description("未登录或 token 失效")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ResultVoid")))))
                        .addResponses("BadRequest", new ApiResponse()
                                .description("参数校验失败或业务错误")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ResultVoid"))))));
    }

    @Bean
    public OpenApiCustomizer sortTagsCustomizer() {
        Map<String, Integer> order = new LinkedHashMap<>();
        order.put("认证", 1);
        order.put("上传", 2);
        order.put("打卡", 3);
        order.put("日记", 4);
        order.put("酒吧", 5);
        order.put("AI", 6);
        order.put("BarBTI", 7);
        order.put("社交", 8);
        order.put("酒款", 9);
        order.put("用户", 10);
        order.put("酒精人格", 11);
        order.put("徽章", 12);
        return openApi -> {
            if (openApi.getTags() != null) {
                openApi.getTags().forEach(tag -> tag.setExtensions(
                        Map.of("x-order", order.getOrDefault(tag.getName(), 99))));
            }
        };
    }
}
