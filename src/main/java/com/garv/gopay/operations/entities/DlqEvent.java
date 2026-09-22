package com.garv.gopay.operations.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dlq_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DlqEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "webhook_event_id", nullable = false)
    private WebhookEvent webhookEvent;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private String finalError;

    private LocalDateTime movedAt;

    private LocalDateTime replayedAt;

}
