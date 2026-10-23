package com.garv.gopay.payment.processor;

import com.garv.gopay.payment.processor.dto.PaymentProcessorRequest;
import com.garv.gopay.payment.processor.dto.PaymentProcessorResponse;
import org.springframework.stereotype.Component;

@Component
public interface PaymentProcessor {
    PaymentProcessorResponse charge(PaymentProcessorRequest paymentProcessorRequest);
}
