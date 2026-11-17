package com.garv.gopay.operations.consumer;

import com.garv.gopay.common.enums.WebhookEventStatus;
import com.garv.gopay.merchant.api.MerchantWebhookApi;
import com.garv.gopay.merchant.dto.response.WebhookConfigResponse;
import com.garv.gopay.operations.dlq.WebhookDlqPublisher;
import com.garv.gopay.operations.entities.WebhookEvent;
import com.garv.gopay.operations.repositories.WebhookEventRepository;
import com.garv.gopay.operations.signature.SignatureData;
import com.garv.gopay.operations.signature.SignatureJson;
import com.garv.gopay.operations.signature.WebhookSigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// consumes the outbox events published by OutboxEventPublisher and fans each one out into a
// WebhookEvent row per enabled merchant config whose eventTypeFilter matches — delivery itself is
// a separate job, this only decides who should be notified and queues it
@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookKafkaConsumer {

    private final MerchantWebhookApi merchantWebhookApi;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDlqPublisher webhookDlqPublisher;

    @KafkaListener(topics = {"${kafka.topics.payment}", "${kafka.topics.order}"}, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onMessage(ConsumerRecord<String, Map<String, Object>> record, Acknowledgment acknowledgment) {
        try {
            if (record.key() == null) {
                log.warn("Skipping event with no merchantId key, offset {} partition {}", record.offset(), record.partition());
                return;
            }

            Map<String, Object> envelope = record.value();
            String eventType = (String) envelope.get("eventType");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
            UUID merchantId = UUID.fromString(record.key());

            List<WebhookConfigResponse> configs = merchantWebhookApi.getActiveByEventType(merchantId, eventType);

            for (WebhookConfigResponse config : configs) {
                try {
                    SignatureData signatureData = new SignatureData(UUID.randomUUID(), System.currentTimeMillis(), payload);
                    String signature = WebhookSigner.sign(config.webhookSecret(), SignatureJson.toJson(signatureData));

                    WebhookEvent webhookEvent = WebhookEvent.builder()
                            .merchantId(merchantId)
                            .eventType(eventType)
                            .payload(payload)
                            .targetUrl(config.targetUrl())
                            .signature(signature)
                            .status(WebhookEventStatus.PENDING)
                            .build();
                    webhookEventRepository.save(webhookEvent);
                } catch (Exception ex) {
                    log.error("Failed to fan out event into webhook_event for config {}, dead-lettering", config.id(), ex);
                    WebhookEvent failedEvent = WebhookEvent.builder()
                            .merchantId(merchantId)
                            .eventType(eventType)
                            .payload(payload)
                            .targetUrl(config.targetUrl())
                            .signature("")
                            .status(WebhookEventStatus.DEAD)
                            .build();
                    webhookDlqPublisher.publish(failedEvent);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to fan out event into webhook_event", ex);
        } finally {
            acknowledgment.acknowledge();
        }
    }

}
