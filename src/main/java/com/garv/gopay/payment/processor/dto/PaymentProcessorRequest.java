package com.garv.gopay.payment.processor.dto;

import com.garv.gopay.common.Money;
import com.garv.gopay.common.enums.PaymentMethod;

import java.util.Map;
import java.util.UUID;

public record PaymentProcessorRequest(
        UUID paymentId,
        UUID processingId,
        PaymentMethod method,
        Money amount,
        String pan,
        String expiry,
        Map<String, Object> methodDetails
) {

    public static PaymentProcessorRequest forCard(UUID paymentId, UUID processingId, PaymentMethod method, Money amount, String pan, String expiry, Map<String, Object> methodDetails) {
        return new PaymentProcessorRequest(paymentId, processingId, method, amount, pan, expiry, methodDetails);
    }

    public static PaymentProcessorRequest forNonCard(UUID paymentId, UUID processingId, PaymentMethod method, Money amount, Map<String, Object> methodDetails) {
        return new PaymentProcessorRequest(paymentId, processingId, method, amount, null, null, methodDetails);
    }

}
