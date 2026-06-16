package com.alcohol.config;

import com.alcohol.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 层配置：JWT 拦截、CORS、上传文件静态映射。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${alcohol.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${alcohol.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/logout",
                        "/api/auth/refresh",
                        "/api/auth/otp/**",
                        "/api/auth/oauth/**",
                        "/api/auth/google/**",
                        "/api/auth/methods",
                        "/api/auth/login/**",
                        "/api/auth/register/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var registration = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
        if ("*".equals(allowedOrigins.trim())) {
            registration.allowedOriginPatterns("*");
        } else {
            registration.allowedOrigins(
                    java.util.Arrays.stream(allowedOrigins.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toArray(String[]::new));
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
