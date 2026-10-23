package com.garv.gopay.payment.dto.response;

import com.garv.gopay.common.Money;
import com.garv.gopay.common.enums.PaymentMethod;
import com.garv.gopay.common.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID merchantId,
        Money amount,
        PaymentStatus status,
        PaymentMethod method,
        Map<String, Object> methodDetails,
        String bankReference,
        String errorCode,
        String errorDescription,
        Money refundedAmount,
        LocalDateTime authorizedAt,
        LocalDateTime capturedAt,
        LocalDateTime settledAt,
        LocalDateTime createdAt
) {
}
