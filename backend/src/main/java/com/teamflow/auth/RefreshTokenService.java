package com.teamflow.auth;

import java.time.Duration;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Stores the current Refresh Token per user in Redis (`refresh:{userId}`),
 * per 09-authentication-authorization.md §2-3. Saving overwrites the
 * previous value, which is how rotation invalidates the old token.
 */
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final Duration expiry;

    public RefreshTokenService(StringRedisTemplate redisTemplate, JwtTokenProvider jwtTokenProvider) {
        this.redisTemplate = redisTemplate;
        this.expiry = jwtTokenProvider.getRefreshTokenExpiry();
    }

    public void save(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, refreshToken, expiry);
    }

    public boolean isValid(Long userId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return Objects.equals(stored, refreshToken);
    }

    public void delete(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
