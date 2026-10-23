package com.garv.gopay.common.ratelimiting.impl;

// NOT USED — not wired into ApiKeyAuthenticationFilter and deliberately not a @Component.
// FixedWindowRateLimiter is the one actually running; this class exists so the Lua-script token bucket
// can be compared side by side with the plain multi-call version in TokenBucketRateLimiterPlain.

import com.garv.gopay.common.ratelimiting.RateLimiter;
import com.garv.gopay.common.ratelimiting.RatelimitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

// token bucket: a bucket holds up to `limit` tokens, refilling continuously over `window` — one request
// costs one token. Unlike the window-based limiters, this lets a caller burst up to the full limit at
// once after being idle, then throttles back to the steady average rate, instead of a hard reset per
// discrete window. The whole read-refill-decrement-write sequence runs as one Lua script so it can't
// suffer the classic token-bucket race: two concurrent requests both reading the same token count,
// both deciding a token is available, and both being allowed — handing out double the real capacity
@Slf4j
@RequiredArgsConstructor
public class TokenBucketRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "ratelimit:bucket:";
    private static final RedisScript<List> SCRIPT =
            RedisScript.of(new ClassPathResource("scripts/token-bucket-rate-limiter.lua"), List.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public RatelimitResult allow(String key, int limit, Duration window) {
        try {
            String redisKey = KEY_PREFIX + key;
            long now = System.currentTimeMillis();

            @SuppressWarnings("unchecked")
            List<Long> result = stringRedisTemplate.execute(
                    SCRIPT,
                    List.of(redisKey),
                    String.valueOf(now), String.valueOf(window.toMillis()), String.valueOf(limit));

            if (result == null || result.size() < 3) {
                log.warn("Rate limit script returned an unexpected result for key {}, allowing the request", key);
                return RatelimitResult.allowed(limit);
            }

            boolean allowed = result.get(0) == 1L;
            int remaining = result.get(1).intValue();
            int retryAfterSeconds = result.get(2).intValue();

            return allowed ? RatelimitResult.allowed(remaining) : RatelimitResult.denied(retryAfterSeconds);
        } catch (Exception ex) {
            // Redis being down should not take down real traffic — fail open rather than reject everything
            log.warn("Rate limit check failed for key {}, allowing the request", key, ex);
            return RatelimitResult.allowed(limit);
        }
    }

}
