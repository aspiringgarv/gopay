package com.garv.gopay.operations.executor;

import com.garv.gopay.common.enums.WebhookEventStatus;
import com.garv.gopay.operations.entities.WebhookEvent;
import com.garv.gopay.operations.queue.WebhookRetryQueue;
import com.garv.gopay.operations.repositories.WebhookEventRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// actually performs the HTTP delivery, off the scheduler thread — one virtual thread per event so
// a slow/hanging target can't stall the rest of the batch WebhookDeliveryPoller just popped
@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookVirtualDeliveryExecutor {

    private static final long RETRY_BACKOFF_BASE_SECONDS = 30;

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookRetryQueue webhookRetryQueue;
    private final RestClient webhookRestClient;
    private ExecutorService executorService;

    @Value("${webhook.delivery.max-attempts:5}")
    private int maxAttempts;

    @PostConstruct
    public void init() {
        executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void submit(UUID webhookEventId) {
        executorService.execute(() -> deliver(webhookEventId));
    }

    private void deliver(UUID id) {
        Optional<WebhookEvent> maybeEvent = webhookEventRepository.findById(id);
        if (maybeEvent.isEmpty()) {
            return;
        }
        WebhookEvent event = maybeEvent.get();

        try {
            Map<String, Object> body = Map.of(
                    "eventType", event.getEventType(),
                    "payload", event.getPayload()
            );

            HttpStatusCode statusCode = webhookRestClient.post()
                    .uri(event.getTargetUrl())
                    .header("X-Webhook-Signature", event.getSignature())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((request, response) -> response.getStatusCode());

            event.setLastResponseCode(statusCode.value());

            if (statusCode.is2xxSuccessful()) {
                event.setStatus(WebhookEventStatus.DELIVERED);
                event.setDeliveredAt(LocalDateTime.now());
            } else {
                markForRetry(event);
            }
        } catch (Exception ex) {
            log.error("Webhook delivery failed for event {}", id, ex);
            event.setLastResponseCode(-1);
            markForRetry(event);
        }

        webhookEventRepository.save(event);
    }

    private void markForRetry(WebhookEvent event) {
        event.setAttempts(event.getAttempts() + 1);
        event.setLastRetryAt(LocalDateTime.now());

        if (event.getAttempts() >= maxAttempts) {
            event.setStatus(WebhookEventStatus.DEAD);
        } else {
            event.setStatus(WebhookEventStatus.PENDING);
            long backoffSeconds = RETRY_BACKOFF_BASE_SECONDS * (1L << (event.getAttempts() - 1));
            LocalDateTime nextRetryAt = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(backoffSeconds);
            event.setNextRetryAt(nextRetryAt);
            webhookRetryQueue.enqueue(event.getId(), nextRetryAt);
        }
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }

}
