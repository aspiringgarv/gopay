package com.garv.gopay.common.ratelimiting.impl;

// NOT USED — kept side by side with TokenBucketRateLimiter for comparison only, not wired up anywhere
// and deliberately not a @Component. The active token-bucket implementation runs the whole
// read-refill-decrement-write sequence as one atomic Lua script instead (see TokenBucketRateLimiter).
// This version does it as separate Redis round trips, which reintroduces the textbook token-bucket
// race: two concurrent requests both HMGET the same token count before either HSETs it back, both see
// a token available, and both get allowed — a genuine lost update, handing out more than `limit` really
// allows. This is a stronger failure than SlidingWindowRateLimiterPlain's boundary-only overcount: here
// the race can repeat on every single concurrent pair of requests, not just at the edge of the window.

import com.garv.gopay.common.ratelimiting.RateLimiter;
import com.garv.gopay.common.ratelimiting.RatelimitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class TokenBucketRateLimiterPlain implements RateLimiter {

    private static final String KEY_PREFIX = "ratelimit:bucket:";
    private static final String TOKENS_FIELD = "tokens";
    private static final String TIMESTAMP_FIELD = "timestamp";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public RatelimitResult allow(String key, int limit, Duration window) {
        try {
            String redisKey = KEY_PREFIX + key;
            HashOperations<String, String, String> hash = stringRedisTemplate.opsForHash();

            long now = System.currentTimeMillis();
            double refillRatePerMs = (double) limit / window.toMillis();

            // read: two separate pieces of state, fetched together but not locked against a concurrent writer
            List<String> raw = hash.multiGet(redisKey, List.of(TOKENS_FIELD, TIMESTAMP_FIELD));
            double tokens = raw.get(0) != null ? Double.parseDouble(raw.get(0)) : limit;
            long lastRefill = raw.get(1) != null ? Long.parseLong(raw.get(1)) : now;

            long elapsedMs = Math.max(0, now - lastRefill);
            tokens = Math.min(limit, tokens + elapsedMs * refillRatePerMs);

            boolean allowed;
            int retryAfterSeconds = 0;

            if (tokens >= 1) {
                tokens -= 1;
                allowed = true;
            } else {
                allowed = false;
                double deficit = 1 - tokens;
                double msUntilNextToken = deficit / refillRatePerMs;
                retryAfterSeconds = (int) Math.max(1, Math.ceil(msUntilNextToken / 1000));
            }

            // write: another request could have read the exact same "before" state above and be about to
            // overwrite whatever we write here — nothing stops both from succeeding on the one token
            hash.putAll(redisKey, Map.of(TOKENS_FIELD, String.valueOf(tokens), TIMESTAMP_FIELD, String.valueOf(now)));
            stringRedisTemplate.expire(redisKey, window.multipliedBy(2));

            return allowed ? RatelimitResult.allowed((int) Math.floor(tokens)) : RatelimitResult.denied(retryAfterSeconds);
        } catch (Exception ex) {
            // Redis being down should not take down real traffic — fail open rather than reject everything
            log.warn("Rate limit check failed for key {}, allowing the request", key, ex);
            return RatelimitResult.allowed(limit);
        }
    }

}
