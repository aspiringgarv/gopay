package com.garv.gopay.operations.entities;

import com.garv.gopay.common.Money;
import com.garv.gopay.common.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "gross_amount_paise", nullable = false)),
            @AttributeOverride(name = "currency", column = @Column(name = "gross_currency", nullable = false))
    })
    private Money grossAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "refund_amount_paise", nullable = false)),
            @AttributeOverride(name = "currency", column = @Column(name = "refund_currency", nullable = false))
    })
    private Money refundAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "fee_amount_paise", nullable = false)),
            @AttributeOverride(name = "currency", column = @Column(name = "fee_currency", nullable = false))
    })
    private Money feeAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "gst_amount_paise", nullable = false)),
            @AttributeOverride(name = "currency", column = @Column(name = "gst_currency", nullable = false))
    })
    private Money gstAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "net_amount_paise", nullable = false)),
            @AttributeOverride(name = "currency", column = @Column(name = "net_currency", nullable = false))
    })
    private Money netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @Column(length = 255)
    private String bankReference;

    private LocalDateTime processedAt;

    private LocalDateTime createdAt;

}
