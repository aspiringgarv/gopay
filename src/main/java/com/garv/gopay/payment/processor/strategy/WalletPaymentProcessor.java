package com.garv.gopay.payment.processor.strategy;

import com.garv.gopay.payment.processor.PaymentProcessor;
import com.garv.gopay.payment.processor.dto.PaymentProcessorRequest;
import com.garv.gopay.payment.processor.dto.PaymentProcessorResponse;
import org.springframework.stereotype.Component;

@Component
public class WalletPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentProcessorResponse charge(PaymentProcessorRequest paymentProcessorRequest) {
        return new PaymentProcessorResponse.Pending(null);
    }

}
