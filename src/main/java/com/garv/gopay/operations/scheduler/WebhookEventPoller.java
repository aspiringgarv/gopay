package com.garv.gopay.operations.scheduler;

import com.garv.gopay.common.enums.WebhookEventStatus;
import com.garv.gopay.operations.entities.WebhookEvent;
import com.garv.gopay.operations.queue.WebhookRetryQueue;
import com.garv.gopay.operations.repositories.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

// picks up PENDING webhook_event rows on its own timer and pushes each id into the Redis retry
// queue for delivery — same split as the outbox's poll/publish, just with Redis as the hand-off
// instead of a direct call
@Component
@RequiredArgsConstructor
public class WebhookEventPoller {

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookRetryQueue webhookRetryQueue;

    @Scheduled(fixedDelayString = "${webhook.retry.polling-interval-ms:20000}")
    public void poll() {
        List<WebhookEvent> pending = webhookEventRepository.findFirst500ByStatusOrderByCreatedAtAsc(WebhookEventStatus.PENDING);

        for (WebhookEvent event : pending) {
            LocalDateTime retryAt = event.getNextRetryAt() != null ? event.getNextRetryAt() : LocalDateTime.now(ZoneOffset.UTC);
            webhookRetryQueue.enqueue(event.getId(), retryAt);
        }
    }

}
