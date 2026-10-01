package com.garv.gopay.payment.entities;

import com.garv.gopay.common.enums.Actor;
import com.garv.gopay.common.enums.PaymentEvent;
import com.garv.gopay.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transition_log", indexes = {
        @Index(name = "idx_payment_transition_log_payment_id", columnList = "payment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransitionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    private PaymentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentEvent paymentEvent;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor", length = 255)
    private Actor actor;

    private String reason;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

}
