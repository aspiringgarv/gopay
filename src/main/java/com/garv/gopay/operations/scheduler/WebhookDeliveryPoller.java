package com.garv.gopay.operations.scheduler;

import com.garv.gopay.operations.executor.WebhookVirtualDeliveryExecutor;
import com.garv.gopay.operations.queue.WebhookRetryQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

// pops ids that are due off the Redis retry queue and hands each one to the
// WebhookVirtualDeliveryExecutor — removed from Redis as soon as they're polled; a failed
// delivery goes back to PENDING with a backed-off nextRetryAt so WebhookEventPoller re-enqueues
// it later, instead of staying in the Redis queue
@Component
@RequiredArgsConstructor
public class WebhookDeliveryPoller {

    private final WebhookRetryQueue webhookRetryQueue;
    private final WebhookVirtualDeliveryExecutor webhookVirtualDeliveryExecutor;

    @Scheduled(fixedDelayString = "${webhook.delivery.polling-interval-ms:2000}")
    public void poll() {
        List<UUID> dueIds = webhookRetryQueue.pollDue();

        for (UUID id : dueIds) {
            webhookVirtualDeliveryExecutor.submit(id);
        }
    }

}
