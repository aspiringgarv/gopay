package com.garv.gopay.operations.dlq;

import com.garv.gopay.common.enums.WebhookEventStatus;
import com.garv.gopay.operations.entities.WebhookDlqRecord;
import com.garv.gopay.operations.entities.WebhookEvent;
import com.garv.gopay.operations.repositories.WebhookDlqRecordRepository;
import com.garv.gopay.operations.repositories.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// publish side of the webhook DLQ: saves a DEAD event's full context into webhook_dlq_record so
// it can be inspected/replayed later, then marks it DEAD_LETTERED so WebhookDlqPoller stops
// picking it up. Row stays DEAD on a save failure, so the next poll just retries it.
// No Kafka for now — straight DB write.
@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookDlqPublisher {

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDlqRecordRepository webhookDlqRecordRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(WebhookEvent event) {
        try {
            WebhookDlqRecord dlqRecord = WebhookDlqRecord.builder()
                    .webhookEventId(event.getId())
                    .merchantId(event.getMerchantId())
                    .eventType(event.getEventType())
                    .targetUrl(event.getTargetUrl())
                    .payload(event.getPayload())
                    .attempts(event.getAttempts())
                    .lastResponseCode(event.getLastResponseCode() == null ? -1 : event.getLastResponseCode())
                    .build();
            webhookDlqRecordRepository.save(dlqRecord);

            event.setStatus(WebhookEventStatus.DEAD_LETTERED);
            webhookEventRepository.save(event);
        } catch (Exception ex) {
            log.error("Failed to record webhook event {} to DLQ", event.getId(), ex);
        }
    }

}
