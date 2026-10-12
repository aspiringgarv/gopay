package com.garv.gopay.payment.processor.router;

import com.garv.gopay.common.enums.PaymentMethod;
import com.garv.gopay.payment.processor.PaymentProcessor;
import com.garv.gopay.payment.processor.dto.PaymentProcessorRequest;
import com.garv.gopay.payment.processor.dto.PaymentProcessorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentProcessorRouter {

    private final Map<PaymentMethod, PaymentProcessor> paymentProcessorMap;

    public PaymentProcessorResponse charge(PaymentProcessorRequest paymentProcessorRequest) {
        return resolve(paymentProcessorRequest.method()).charge(paymentProcessorRequest);
    }

    private PaymentProcessor resolve(PaymentMethod method) {
        PaymentProcessor processor = paymentProcessorMap.get(method);
        if (processor == null) {
            throw new IllegalStateException("No PaymentProcessor configured for method: " + method);
        }
        return processor;
    }

}
