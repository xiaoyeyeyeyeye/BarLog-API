package com.alcohol.interceptor;

import com.alcohol.compat.CompatAuthSupport;
import com.alcohol.config.CompatProperties;
import com.alcohol.context.UserContext;
import com.alcohol.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;

/**
 * JWT 认证拦截器：解析 Bearer Token，将 userId 写入 {@link com.alcohol.context.UserContext}。
 * <p>公开路径见 {@link #PUBLIC_PREFIXES}；开发模式下可匿名回落 demo 用户（见 {@link CompatProperties}）。</p>
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final CompatAuthSupport compatAuthSupport;
    private final ObjectMapper objectMapper;

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/google/",
            "/swagger-ui/",
            "/v3/api-docs",
            "/error",
            "/health",
            "/uploads/",
            "/api/media/"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        if (PUBLIC_PREFIXES.stream().anyMatch(uri::startsWith)) {
            return true;
        }

        String token = extractToken(request);
        if (token != null && jwtUtil.validateToken(token) && !jwtUtil.isTokenExpired(token)) {
            UserContext.setUserId(jwtUtil.getUserIdFromToken(token));
            return true;
        }

        if (compatAuthSupport.tryAnonymousAuth()) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "message", "Missing bearer token",
                "code", "AUTH_REQUIRED")));
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}

