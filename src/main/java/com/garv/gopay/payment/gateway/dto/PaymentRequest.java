package com.garv.gopay.payment.gateway.dto;

import com.garv.gopay.common.Money;
import com.garv.gopay.common.enums.PaymentMethod;

import java.util.Map;
import java.util.UUID;

public record PaymentRequest(
        UUID paymentId,
        UUID orderId,
        Money amount,
        PaymentMethod method,
        Map<String, Object> methodDetails
) {
}
