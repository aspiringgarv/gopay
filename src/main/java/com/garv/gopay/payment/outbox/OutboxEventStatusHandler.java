package com.garv.gopay.payment.outbox;

import com.garv.gopay.common.enums.OutboxEventStatus;
import com.garv.gopay.payment.entities.OutboxEvent;
import com.garv.gopay.payment.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Pulled out of OutboxEventPublisher as its own component specifically so @Transactional actually
// applies: Spring's @Transactional works through a proxy around public methods called from
// outside the class, so it would silently do nothing on a private method (or a public method
// called via `this.` from within the same class).
@Component
@RequiredArgsConstructor
public class OutboxEventStatusHandler {

    private final OutboxEventRepository outboxEventRepository;

    @Value("${outbox.publisher.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public void markPublished(OutboxEvent event) {
        event.setStatus(OutboxEventStatus.PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());
        event.setErrorMessage(null);
        outboxEventRepository.save(event);
    }

    // past maxAttempts, the row moves to FAILED (terminal) instead of staying PENDING forever —
    // a permanently-broken payload no longer retries indefinitely
    @Transactional
    public void markFailed(OutboxEvent event, String errorMessage) {
        int retryCount = event.getRetryCount() + 1;
        event.setRetryCount(retryCount);
        event.setErrorMessage(errorMessage);
        if (retryCount >= maxAttempts) {
            event.setStatus(OutboxEventStatus.FAILED);
        }
        outboxEventRepository.save(event);
    }

}
