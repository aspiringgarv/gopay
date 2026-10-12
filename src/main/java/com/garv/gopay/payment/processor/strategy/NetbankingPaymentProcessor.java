package com.garv.gopay.payment.processor.strategy;

import com.garv.gopay.common.util.RandomizerUtil;
import com.garv.gopay.payment.processor.PaymentProcessor;
import com.garv.gopay.payment.processor.dto.PaymentProcessorRequest;
import com.garv.gopay.payment.processor.dto.PaymentProcessorResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class NetbankingPaymentProcessor implements PaymentProcessor {

    // magic test bank code for a deterministic decline in demos
    private static final String ALWAYS_DECLINE_BANK_CODE = "FAIL001";

    @Override
    public PaymentProcessorResponse charge(PaymentProcessorRequest paymentProcessorRequest) {
        Object bankCode = paymentProcessorRequest.methodDetails() != null
                ? paymentProcessorRequest.methodDetails().get("bankCode")
                : null;
        if (ALWAYS_DECLINE_BANK_CODE.equals(bankCode)) {
            return new PaymentProcessorResponse.Failure("NBK_DECLINED", "Bank declined the transaction");
        }

        // no real bank server to call, so simulate the range of outcomes a real netbanking gateway could return
        String processorRef = "NBKPROC_" + RandomizerUtil.randomBase64(16);
        double outcome = ThreadLocalRandom.current().nextDouble();

        if (outcome < 0.7) {
            return new PaymentProcessorResponse.Pending(processorRef);
        } else {
            return new PaymentProcessorResponse.Failure("NBK_DECLINED", "Bank declined the transaction");
        }
    }

}
