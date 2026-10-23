package com.garv.gopay.common.ratelimiting.impl;

// NOT USED — not wired into ApiKeyAuthenticationFilter and deliberately not a @Component.
// FixedWindowRateLimiter is the one actually running; this class exists so the Lua-script sliding window
// can be compared side by side with the plain multi-call version in SlidingWindowRateLimiterPlain.

import com.garv.gopay.common.ratelimiting.RateLimiter;
import com.garv.gopay.common.ratelimiting.RatelimitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

// sliding window log, same idea as before (every request's timestamp in a Redis sorted set, scored by
// when it happened), but the whole prune + add + count + find-oldest sequence now runs as ONE Lua script
// on the Redis server instead of four separate round trips. Lua scripts execute atomically in Redis — no
// other command runs while one is in flight — which closes the boundary race the plain Java version has
// when several concurrent requests for the same key interleave their calls
@Slf4j
@RequiredArgsConstructor
public class SlidingWindowRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "ratelimit:sliding:";
    private static final RedisScript<List> SCRIPT =
            RedisScript.of(new ClassPathResource("scripts/sliding-window-rate-limiter.lua"), List.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public RatelimitResult allow(String key, int limit, Duration window) {
        try {
            String redisKey = KEY_PREFIX + key;
            long now = System.currentTimeMillis();
            String member = now + "-" + UUID.randomUUID();

            @SuppressWarnings("unchecked")
            List<Long> result = stringRedisTemplate.execute(
                    SCRIPT,
                    List.of(redisKey),
                    String.valueOf(now), String.valueOf(window.toMillis()), String.valueOf(limit), member);

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
