package com.garv.gopay.payment.gateway.adapter;

import com.garv.gopay.payment.gateway.PaymentAdapter;
import com.garv.gopay.payment.gateway.dto.PaymentRequest;
import com.garv.gopay.payment.gateway.dto.PaymentResult;
import com.garv.gopay.payment.processor.dto.PaymentProcessorRequest;
import com.garv.gopay.payment.processor.dto.PaymentProcessorResponse;
import com.garv.gopay.payment.processor.router.PaymentProcessorRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class UpiPaymentAdapter implements PaymentAdapter {

    private final PaymentProcessorRouter paymentProcessorRouter;

    @Override
    public PaymentResult initiate(PaymentRequest payment) {
        PaymentProcessorRequest processorRequest = PaymentProcessorRequest.forNonCard(
                payment.paymentId(),
                UUID.randomUUID(),
                payment.method(),
                payment.amount(),
                payment.methodDetails()
        );

        try {
            log.info("Charging upi payment for paymentId {}, processingId {}", processorRequest.paymentId(), processorRequest.processingId());
            PaymentProcessorResponse processorResponse = paymentProcessorRouter.charge(processorRequest);

            return switch (processorResponse) {
                case PaymentProcessorResponse.Pending pending -> new PaymentResult.Pending(pending.processorRef());
                case PaymentProcessorResponse.Success success -> new PaymentResult.Success(success.bankRef());
                case PaymentProcessorResponse.Failure failure -> new PaymentResult.Failure(failure.errorCode(), failure.errorDescription());
            };
        } catch (Exception ex) {
            log.error("Upi processor call failed for paymentId {}", payment.paymentId(), ex);
            return new PaymentResult.Failure("UPI_FAILED", ex.getMessage());
        }
    }

    @Override
    public PaymentResult capture(PaymentRequest payment) {
        // upi settles at initiate time, nothing further to capture
        return new PaymentResult.Success(null);
    }

}
