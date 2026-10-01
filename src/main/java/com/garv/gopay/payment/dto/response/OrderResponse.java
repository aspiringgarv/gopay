package com.garv.gopay.payment.dto.response;

import com.garv.gopay.common.Money;
import com.garv.gopay.common.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID merchantId,
        Money amount,
        OrderStatus status,
        String receipt,
        Map<String, Object> notes,
        Integer attempts,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}
