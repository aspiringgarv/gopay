package com.garv.gopay.common.ratelimiting.impl;

// NOT USED — kept side by side with SlidingWindowRateLimiter for comparison only, not wired up anywhere
// and deliberately not a @Component. The active sliding-window implementation runs the same algorithm as
// one atomic Lua script instead (see SlidingWindowRateLimiter); this version does it as four separate
// Redis round trips, which means a boundary race under real concurrency — see that class's header comment
// and the "is it thread safe" discussion this was built to answer.

import com.garv.gopay.common.ratelimiting.RateLimiter;
import com.garv.gopay.common.ratelimiting.RatelimitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

// sliding window log: every request's timestamp lives in a Redis sorted set, scored by when it happened.
// on each call we drop everything older than the window, then count what's left — a true rolling window,
// not fixed buckets, so there is no boundary burst like FixedWindowRateLimiter has. Costs more Redis work
// per check (prune + add + count, vs one INCR) in exchange for that precision.
@Slf4j
@RequiredArgsConstructor
public class SlidingWindowRateLimiterPlain implements RateLimiter {

    private static final String KEY_PREFIX = "ratelimit:sliding:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public RatelimitResult allow(String key, int limit, Duration window) {
        try {
            String redisKey = KEY_PREFIX + key;
            ZSetOperations<String, String> zSet = stringRedisTemplate.opsForZSet();

            long now = System.currentTimeMillis();
            long floorMs = now - window.toMillis();

            // drop everything that has already aged out of the window before counting what's left
            zSet.removeRangeByScore(redisKey, 0, floorMs);

            // the score is what actually places this request in time; the member just has to be unique,
            // since a ZSET silently collapses two entries that share a member (two requests in the same
            // millisecond would otherwise overwrite each other's score instead of both being counted)
            zSet.add(redisKey, now + "-" + UUID.randomUUID(), now);

            // the key should not outlive the window once this caller goes quiet
            stringRedisTemplate.expire(redisKey, window);

            long count = zSet.zCard(redisKey) != null ? zSet.zCard(redisKey) : 0L;

            if (count <= limit) {
                return RatelimitResult.allowed((int) (limit - count));
            }

            int retryAfterSeconds = retryAfterSeconds(zSet, redisKey, window, now);
            return RatelimitResult.denied(retryAfterSeconds);
        } catch (Exception ex) {
            // Redis being down should not take down real traffic — fail open rather than reject everything
            log.warn("Rate limit check failed for key {}, allowing the request", key, ex);
            return RatelimitResult.allowed(limit);
        }
    }

    // the window frees up exactly when its oldest surviving entry ages out, not just "the whole window from now"
    private int retryAfterSeconds(ZSetOperations<String, String> zSet, String redisKey, Duration window, long now) {
        Set<ZSetOperations.TypedTuple<String>> oldest = zSet.rangeWithScores(redisKey, 0, 0);
        if (oldest == null || oldest.isEmpty() || oldest.iterator().next().getScore() == null) {
            return (int) window.toSeconds();
        }

        long oldestTimestamp = oldest.iterator().next().getScore().longValue();
        long msUntilFree = (oldestTimestamp + window.toMillis()) - now;
        return (int) Math.max(1, msUntilFree / 1000);
    }

}
