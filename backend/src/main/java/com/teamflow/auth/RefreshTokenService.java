package com.teamflow.auth;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if current ~= ARGV[1] then return 0 end
            redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration expiry;

    public RefreshTokenService(StringRedisTemplate redisTemplate, JwtTokenProvider jwtTokenProvider) {
        this.redisTemplate = redisTemplate;
        this.expiry = jwtTokenProvider.getRefreshTokenExpiry();
    }

    public void save(Long userId, String refreshToken) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + userId, refreshToken, expiry);
        } catch (RuntimeException ex) {
            throw unavailable();
        }
    }

    public boolean isValid(Long userId, String refreshToken) {
        try {
            String stored = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
            return Objects.equals(stored, refreshToken);
        } catch (RuntimeException ex) {
            throw unavailable();
        }
    }

    /** Atomically consumes the presented token and replaces it with the next token. */
    public boolean rotate(Long userId, String presentedToken, String replacementToken) {
        try {
            Long result = redisTemplate.execute(ROTATE_SCRIPT, List.of(KEY_PREFIX + userId),
                    presentedToken, replacementToken, String.valueOf(expiry.toMillis()));
            return Long.valueOf(1L).equals(result);
        } catch (RuntimeException ex) {
            throw unavailable();
        }
    }

    public void delete(Long userId) {
        try {
            redisTemplate.delete(KEY_PREFIX + userId);
        } catch (RuntimeException ex) {
            throw unavailable();
        }
    }

    private BusinessException unavailable() {
        return new BusinessException(ErrorCode.AUTH_SERVICE_UNAVAILABLE);
    }
}
