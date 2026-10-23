package com.garv.gopay.payment.entities;

import com.garv.gopay.common.Money;
import com.garv.gopay.common.entities.BaseEntity;
import com.garv.gopay.common.enums.PaymentMethod;
import com.garv.gopay.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payment", indexes = {
        @Index(name = "idx_payment_order_id", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderRecord order;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private String idempotencyKey;

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> methodDetails;

    @Column(length = 255)
    private String processorReference;

    @Column(length = 255)
    private String bankReference;

    @Column(length = 255)
    private String errorCode;

    @Column(length = 255)
    private String errorDescription;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "refunded_amount_paise", nullable = false)),
            @AttributeOverride(name = "currency", column = @Column(name = "refunded_currency", nullable = false))
    })
    @Builder.Default
    private Money refundedAmount = Money.of(0, null);

    private LocalDateTime authorizedAt;

    private LocalDateTime capturedAt;

    private LocalDateTime settledAt;

}
