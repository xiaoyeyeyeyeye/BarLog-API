package com.alcohol.context;

/**
 * 当前请求登录用户 ID（ThreadLocal）。
 * <p>由 {@link com.alcohol.interceptor.AuthInterceptor} 写入，请求结束时必须 {@link #clear()}。</p>
 */
public final class UserContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
