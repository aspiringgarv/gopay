package com.garv.gopay.operations.entities;

import com.garv.gopay.common.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

// durable record of what landed in the webhook-dlq-events Kafka topic — kept in its own table so
// a DLQ'd delivery can be inspected/replayed without having to replay the Kafka topic itself
@Entity
@Table(name = "webhook_dlq_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDlqRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "webhook_event_id", nullable = false)
    private UUID webhookEventId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String targetUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    @Column(nullable = false)
    private int attempts;

    private Integer lastResponseCode;

}
