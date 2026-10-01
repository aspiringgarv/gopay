package com.garv.gopay.payment.dto.request;

import com.garv.gopay.common.Money;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;

public record CreateOrderRequest(
        @NotNull(message = "Amount is required")
        Money amount,

        @Size(max = 100, message = "Receipt must have at most 100 characters")
        String receipt, ///  order-id known to merchant

        @Size(max = 100, message = "Notes must have at most 100 entries")
        Map<String, Object> notes,

        LocalDateTime expiresAt
) {
}
