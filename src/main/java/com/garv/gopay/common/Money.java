package com.garv.gopay.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Money {

    @Column(name = "amount_paise", nullable = false)
    private long amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    public Money(long amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(long amount, String currency) {
        return new Money(amount, currency);
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        return Money.of(this.amount + other.amount, this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        return Money.of(this.amount - other.amount, this.currency);
    }

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot operate on Money with different currencies: " + this.currency + " and " + other.currency);
        }
    }

}
