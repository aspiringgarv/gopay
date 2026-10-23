package com.garv.gopay.common.ratelimiting.impl;

import com.garv.gopay.common.ratelimiting.RateLimiter;
import com.garv.gopay.common.ratelimiting.RatelimitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

// classic fixed-window counter: INCR a key scoped to the caller, EXPIRE it only on the first hit in the window.
// simple and cheap, but bursty at window edges (a caller can send 2x the limit by timing requests around
// the boundary) — good enough here; a sliding window would smooth that out at the cost of more Redis calls
@Slf4j
@Component
@RequiredArgsConstructor
public class FixedWindowRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public RatelimitResult allow(String key, int limit, Duration window) {
        try {
            String redisKey = KEY_PREFIX + key;
            Long count = stringRedisTemplate.opsForValue().increment(redisKey);
            if (count == null) {
                // the client contract allows a null reply; treat it the same as any other Redis failure
                log.warn("Rate limit increment returned null for key {}, allowing the request", key);
                return RatelimitResult.allowed(limit);
            }

            if (count == 1L) {
                // only the request that opened this window sets its lifetime, so it isn't reset on every hit
                stringRedisTemplate.expire(redisKey, window);
            }

            if (count <= limit) {
                return RatelimitResult.allowed(limit - count.intValue());
            }

            Long ttlSeconds = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            int retryAfterSeconds = (ttlSeconds != null && ttlSeconds > 0) ? ttlSeconds.intValue() : (int) window.toSeconds();
            return RatelimitResult.denied(retryAfterSeconds);
        } catch (Exception ex) {
            // Redis being down should not take down real traffic — fail open rather than reject everything.
            // "remaining" is meaningless here since the real count is unknown, so it just echoes the limit back
            log.warn("Rate limit check failed for key {}, allowing the request", key, ex);
            return RatelimitResult.allowed(limit);
        }
    }

}
