package com.garv.gopay.payment.processor.strategy;

import com.garv.gopay.common.util.RandomizerUtil;
import com.garv.gopay.payment.processor.PaymentProcessor;
import com.garv.gopay.payment.processor.dto.PaymentProcessorRequest;
import com.garv.gopay.payment.processor.dto.PaymentProcessorResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class UpiPaymentProcessor implements PaymentProcessor {

    // magic test VPA for a deterministic decline in demos
    private static final String ALWAYS_DECLINE_VPA = "fail@upi";

    @Override
    public PaymentProcessorResponse charge(PaymentProcessorRequest paymentProcessorRequest) {
        Object vpa = paymentProcessorRequest.methodDetails() != null
                ? paymentProcessorRequest.methodDetails().get("vpa")
                : null;
        if (ALWAYS_DECLINE_VPA.equals(vpa)) {
            return new PaymentProcessorResponse.Failure("UPI_DECLINED", "UPI transaction declined");
        }

        // no real UPI switch to call, so simulate the range of outcomes a real UPI gateway could return
        String processorRef = "UPIPROC_" + RandomizerUtil.randomBase64(16);
        double outcome = ThreadLocalRandom.current().nextDouble();

        if (outcome < 0.7) {
            return new PaymentProcessorResponse.Pending(processorRef);
        } else {
            return new PaymentProcessorResponse.Failure("UPI_DECLINED", "UPI transaction declined");
        }
    }

}
