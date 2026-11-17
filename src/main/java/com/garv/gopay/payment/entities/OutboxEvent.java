package com.garv.gopay.payment.entities;

import com.garv.gopay.common.entities.BaseEntity;
import com.garv.gopay.common.enums.AggregateType;
import com.garv.gopay.common.enums.OutboxEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

// Transactional outbox: a row here is written in the same DB transaction as the business change
// it describes (e.g. a Payment status flip), so the event can never be lost even if the app
// crashes right after committing and before it reaches Kafka. A separate relay polls for
// PENDING rows, resolves the topic from aggregateType via KafkaProperties, publishes, and flips
// it to PUBLISHED/FAILED — nothing here talks to Kafka directly.
@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_outbox_event_status", columnList = "status"),
        @Index(name = "idx_outbox_event_aggregate", columnList = "aggregate_type, aggregate_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private AggregateType aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @Builder.Default
    private int retryCount = 0;

    @Column(columnDefinition = "text")
    private String errorMessage;

    private LocalDateTime publishedAt;

}
