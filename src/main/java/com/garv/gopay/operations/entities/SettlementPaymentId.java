package com.garv.gopay.operations.entities;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class SettlementPaymentId implements Serializable {
    private UUID settlementId;
    private UUID paymentId;
}
