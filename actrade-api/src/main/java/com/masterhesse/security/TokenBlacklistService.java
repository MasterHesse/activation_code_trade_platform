package com.masterhesse.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务
 * 用于存储已撤销的 Token，实现登出功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    /**
     * 将 Token 加入黑名单
     * @param token JWT Token
     * @param remainingSeconds 剩余有效时间（秒）
     */
    public void blacklistToken(String token, long remainingSeconds) {
        if (remainingSeconds <= 0) {
            return; // 已过期的 Token 不需要加入黑名单
        }

        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "revoked", remainingSeconds, TimeUnit.SECONDS);
        log.debug("Token added to blacklist: {} (expires in {}s)", key, remainingSeconds);
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 将用户的所有 Token 加入黑名单
     * @param username 用户名
     */
    public void blacklistAllUserTokens(String username) {
        // 可以存储一个用户撤销时间，后续生成的 Token 检查是否在撤销时间之后
        String key = "jwt:revoke:" + username;
        redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()));
        log.info("All tokens for user {} have been revoked", username);
    }

    /**
     * 获取用户 Token 撤销时间
     * @param username 用户名
     * @return 撤销时间戳，null 表示未撤销
     */
    public Long getUserRevokeTime(String username) {
        String key = "jwt:revoke:" + username;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : null;
    }
}
