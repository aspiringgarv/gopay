package com.garv.gopay.operations.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebhookRetryQueue {

    private static final String QUEUE_KEY = "webhook:retry:queue";

    private final StringRedisTemplate stringRedisTemplate;

    // ZADD NX — member (the event id) only gets added if it isn't already in the set, so a
    // still-PENDING row seen again on a later poll is a no-op instead of a duplicate entry.
    // Score is retryAt as epoch millis, so the queue stays poppable due-first (ZRANGE/ZPOPMIN).
    public void enqueue(UUID webhookEventId, LocalDateTime retryAt) {
        long score = retryAt.toInstant(ZoneOffset.UTC).toEpochMilli();
        stringRedisTemplate.opsForZSet().addIfAbsent(QUEUE_KEY, webhookEventId.toString(), score);
    }

    public void remove(UUID webhookEventId) {
        stringRedisTemplate.opsForZSet().remove(QUEUE_KEY, webhookEventId.toString());
    }

    // members with score <= now — i.e. retryAt has already passed, so they're due for delivery.
    // Removed from the queue right here so a slow delivery can't get the same id handed out twice
    // on the next poll.
    public List<UUID> pollDue() {
        long now = Instant.now().toEpochMilli();
        Set<ZSetOperations.TypedTuple<String>> due = stringRedisTemplate.opsForZSet().rangeByScoreWithScores(QUEUE_KEY, 0, now);
        if (due == null || due.isEmpty()) {
            return List.of();
        }
        List<UUID> dueIds = due.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                .toList();

        dueIds.forEach(this::remove);
        return dueIds;
    }

}
