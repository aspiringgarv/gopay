package com.garv.gopay.operations.entities;

import com.garv.gopay.common.entities.BaseEntity;
import com.garv.gopay.payment.entities.Payment;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "settlement_payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementPayment extends BaseEntity {
    @EmbeddedId
    private SettlementPaymentId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("settlementId")
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("paymentId")
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

}
