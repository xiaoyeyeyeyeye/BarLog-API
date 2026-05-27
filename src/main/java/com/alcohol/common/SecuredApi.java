package com.alcohol.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * 标记需要 JWT 的接口（Swagger 文档用）
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SecurityRequirement(name = "BearerAuth")
public @interface SecuredApi {
}
