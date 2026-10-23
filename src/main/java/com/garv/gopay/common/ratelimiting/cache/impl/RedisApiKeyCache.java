package com.garv.gopay.common.ratelimiting.cache.impl;

import com.garv.gopay.common.ratelimiting.cache.ApiKeyCache;
import com.garv.gopay.common.ratelimiting.cache.ApiKeyCacheEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisApiKeyCache implements ApiKeyCache {

    private static final String KEY_PREFIX = "apikey:";
    private static final Duration BASE_TTL = Duration.ofMinutes(5);
    // spreads expiry over time so entries cached around the same moment don't all miss together (cache stampede)
    private static final Duration JITTER = Duration.ofSeconds(60);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // the cache is best-effort: Redis being down or a bad entry should fall back to the DB, never break auth
    @Override
    public Optional<ApiKeyCacheEntry> get(String keyId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(redisKey(keyId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, ApiKeyCacheEntry.class));
        } catch (Exception ex) {
            log.warn("Failed to read API key cache entry for keyId {}, treating as a cache miss", keyId, ex);
            return Optional.empty();
        }
    }

    @Override
    public void put(String keyId, ApiKeyCacheEntry entry) {
        try {
            stringRedisTemplate.opsForValue().set(redisKey(keyId), objectMapper.writeValueAsString(entry), ttlWithJitter());
        } catch (Exception ex) {
            log.warn("Failed to write API key cache entry for keyId {}, continuing without caching it", keyId, ex);
        }
    }

    private Duration ttlWithJitter() {
        long jitterMs = ThreadLocalRandom.current().nextLong(-JITTER.toMillis(), JITTER.toMillis() + 1);
        return BASE_TTL.plusMillis(jitterMs);
    }

    @Override
    public void evict(String keyId) {
        try {
            stringRedisTemplate.delete(redisKey(keyId));
        } catch (Exception ex) {
            log.warn("Failed to evict API key cache entry for keyId {}", keyId, ex);
        }
    }

    private String redisKey(String keyId) {
        return KEY_PREFIX + keyId;
    }

}
