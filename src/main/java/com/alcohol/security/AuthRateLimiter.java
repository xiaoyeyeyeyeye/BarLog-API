package com.alcohol.security;

import com.alcohol.common.BizException;
import com.alcohol.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存限流（单实例）。防暴力破解登录/注册/OTP；多实例部署需换 Redis。
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimiter {

    private final SecurityProperties securityProperties;

    private final Map<String, Deque<Long>> requestBuckets = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> failedLoginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> registerBuckets = new ConcurrentHashMap<>();

    public void checkAuthRequest(String clientIp) {
        if (!securityProperties.getRateLimit().isEnabled()) {
            return;
        }
        int limit = securityProperties.getRateLimit().getMaxRequestsPerMinutePerIp();
        enforceLimit(requestBuckets, key("req", clientIp), limit, 60, "Too many auth requests, try again later");
    }

    public void recordFailedLogin(String clientIp) {
        if (!securityProperties.getRateLimit().isEnabled()) {
            return;
        }
        int limit = securityProperties.getRateLimit().getMaxFailedLoginsPerIp();
        int windowSec = securityProperties.getRateLimit().getFailedLoginWindowMinutes() * 60;
        enforceLimit(failedLoginBuckets, key("fail", clientIp), limit, windowSec,
                "Too many failed login attempts, try again later");
    }

    public void checkRegistration(String clientIp) {
        if (!securityProperties.getRateLimit().isEnabled()) {
            return;
        }
        int limit = securityProperties.getRateLimit().getMaxRegistrationsPerIpPerHour();
        enforceLimit(registerBuckets, key("reg", clientIp), limit, 3600,
                "Too many registration attempts from this network");
    }

    private void enforceLimit(Map<String, Deque<Long>> store, String bucketKey, int maxEvents,
                              int windowSeconds, String message) {
        long now = Instant.now().getEpochSecond();
        Deque<Long> deque = store.computeIfAbsent(bucketKey, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() <= now - windowSeconds) {
                deque.pollFirst();
            }
            if (deque.size() >= maxEvents) {
                throw new BizException(message, 429);
            }
            deque.addLast(now);
        }
    }

    private static String key(String prefix, String clientIp) {
        return prefix + ":" + (clientIp == null || clientIp.isBlank() ? "unknown" : clientIp);
    }
}
