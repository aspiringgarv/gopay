package com.garv.gopay.payment.gateway.adapter;

import com.garv.gopay.payment.gateway.PaymentAdapter;
import com.garv.gopay.payment.gateway.dto.PaymentRequest;
import com.garv.gopay.payment.gateway.dto.PaymentResult;
import com.garv.gopay.payment.processor.dto.PaymentProcessorResponse;
import com.garv.gopay.vault.services.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CardPaymentAdapter implements PaymentAdapter {

    private final VaultService vaultService;

    @Override
    public PaymentResult initiate(PaymentRequest paymentRequest) {
        String token = (String) paymentRequest.methodDetails().get("token");

        try {
            log.info("Charging card payment for paymentId {}, token {}", paymentRequest.paymentId(), token);
            PaymentProcessorResponse processorResponse = vaultService.charge(token, paymentRequest);

            return switch (processorResponse) {
                case PaymentProcessorResponse.Pending pending -> new PaymentResult.Pending(pending.processorRef());
                case PaymentProcessorResponse.Success success -> new PaymentResult.Success(success.bankRef());
                case PaymentProcessorResponse.Failure failure -> new PaymentResult.Failure(failure.errorCode(), failure.errorDescription());
            };
        } catch (Exception ex) {
            log.error("Card processor call failed for paymentId {}", paymentRequest.paymentId(), ex);
            return new PaymentResult.Failure("CARD_FAILED", ex.getMessage());
        }
    }

    @Override
    public PaymentResult capture(PaymentRequest paymentRequest) {
        return new PaymentResult.Success(null);
    }

}
