package com.garv.gopay.payment.outbox;

import com.garv.gopay.common.enums.OutboxEventStatus;
import com.garv.gopay.payment.entities.OutboxEvent;
import com.garv.gopay.payment.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// Scheduling side of the transactional outbox: on its own timer, fetches a batch of PENDING rows
// and hands each one to OutboxEventPublisher.publish() to actually reach Kafka.
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Scheduled(fixedDelayString = "${outbox.publisher.polling-interval-ms:5000}")
    public void poll() {
        List<OutboxEvent> pending = outboxEventRepository.findFirst500ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        for (OutboxEvent event : pending) {
            outboxEventPublisher.publish(event);
        }
    }

}
