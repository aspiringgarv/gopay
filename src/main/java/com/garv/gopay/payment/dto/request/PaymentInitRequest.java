package com.garv.gopay.payment.dto.request;

import com.garv.gopay.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record PaymentInitRequest(
        @NotNull(message = "Order id is required")
        UUID orderId,

        @NotNull(message = "Payment method is required")
        PaymentMethod method,

        @Size(max = 100, message = "Method methodDetails must have at most 100 entries")
        Map<String, Object> methodDetails
) {
}
