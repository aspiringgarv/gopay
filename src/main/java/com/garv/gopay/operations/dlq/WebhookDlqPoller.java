package com.garv.gopay.operations.dlq;

import com.garv.gopay.common.enums.WebhookEventStatus;
import com.garv.gopay.operations.entities.WebhookEvent;
import com.garv.gopay.operations.repositories.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// on its own timer, fetches a batch of DEAD rows (exhausted all delivery retries) and hands each
// one to WebhookDlqPublisher to actually reach the DLQ topic
@Component
@RequiredArgsConstructor
public class WebhookDlqPoller {

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDlqPublisher webhookDlqPublisher;

    @Scheduled(fixedDelayString = "${webhook.dlq.polling-interval-ms:30000}")
    public void poll() {
        List<WebhookEvent> dead = webhookEventRepository.findFirst500ByStatusOrderByCreatedAtAsc(WebhookEventStatus.DEAD);

        for (WebhookEvent event : dead) {
            webhookDlqPublisher.publish(event);
        }
    }

}
