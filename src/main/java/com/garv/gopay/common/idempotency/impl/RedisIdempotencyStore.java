package com.garv.gopay.common.idempotency.impl;

import com.garv.gopay.common.idempotency.IdempotencyStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String KEY_PREFIX = "idempotency:";

    private final StringRedisTemplate stringRedisTemplate;

    // NOT caught here, deliberately: this is the gate deciding whether real work happens at all.
    // Guessing wrong on a Redis failure is unsafe either way — pretending the key was absent could let
    // a payment be processed twice; pretending it was already claimed could block every first attempt.
    // So this propagates as a real exception, and a client retrying with the same key is the correct
    // recovery once Redis is back.
    @Override
    public boolean setIfAbsent(String key, Duration ttl) {
        Boolean stored = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + key, IdempotencyStore.IN_PROGRESS, ttl);
        return Boolean.TRUE.equals(stored);
    }

    // by the time this runs the real work already succeeded — a Redis failure here must not fail the
    // request for an operation that actually completed, so it is logged and swallowed, not propagated
    @Override
    public void store(String key, String value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + key, value, ttl);
        } catch (Exception ex) {
            log.warn("Failed to store idempotency result for key {}, a replay will not see it", key, ex);
        }
    }

    // called only after setIfAbsent already established this is a duplicate — failing to fetch the
    // stored result is a degraded replay, not a correctness risk, so this returns empty instead of
    // failing the whole request
    @Override
    public Optional<String> get(String key) {
        try {
            return Optional.ofNullable(stringRedisTemplate.opsForValue().get(KEY_PREFIX + key));
        } catch (Exception ex) {
            log.warn("Failed to read idempotency result for key {}", key, ex);
            return Optional.empty();
        }
    }

    // runs during cleanup after a failure — a second, unrelated Redis failure here must not mask the
    // real exception the caller is already in the middle of propagating
    @Override
    public void delete(String key) {
        try {
            stringRedisTemplate.delete(KEY_PREFIX + key);
        } catch (Exception ex) {
            log.warn("Failed to release idempotency key {}, it will stay claimed until its TTL expires", key, ex);
        }
    }

}
