package com.garv.gopay.payment.outbox;

import com.garv.gopay.common.config.KafkaProperties;
import com.garv.gopay.common.enums.AggregateType;
import com.garv.gopay.payment.entities.OutboxEvent;
import com.garv.gopay.payment.repositories.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// Write + send sides of the transactional outbox. enqueue() is called from a service inside its
// own @Transactional method, same transaction as the business save — just a plain insert, no
// Kafka call happens there. publish() is the only thing in the codebase that actually talks to
// Kafka; it's called later, on its own schedule, by OutboxPoller. The DB write after a send is
// delegated to OutboxEventStatusHandler, a separate bean, so @Transactional on it actually takes
// effect (it wouldn't on a private/same-class method call).
@Component
@Slf4j
public class OutboxEventPublisher {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventStatusHandler outboxEventStatusHandler;

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private KafkaProperties kafkaProperties;

    public void enqueue(AggregateType aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload) {
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .build();
        outboxEventRepository.save(outboxEvent);
    }

    // row stays PENDING on failure so the next poll retries it, up to
    // outbox.publisher.max-attempts — past that, OutboxEventStatusHandler moves it to FAILED
    public void publish(OutboxEvent event) {
        try {
            String topic = kafkaProperties.getTopic(event.getAggregateType().name());
            String merchantId = (String) event.getPayload().get("merchantId");
            Map<String, Object> envelope = Map.of(
                    "eventType", event.getEventType(),
                    "aggregateType", event.getAggregateType(),
                    "aggregateId", event.getAggregateId(),
                    "payload", event.getPayload()
            );

            kafkaTemplate.send(topic, merchantId, envelope).get(5, TimeUnit.SECONDS);

            outboxEventStatusHandler.markPublished(event);
        } catch (Exception ex) {
            log.error("Failed to publish outbox event {} (aggregateType {})", event.getId(), event.getAggregateType(), ex);
            outboxEventStatusHandler.markFailed(event, ex.getMessage());
        }
    }

}
