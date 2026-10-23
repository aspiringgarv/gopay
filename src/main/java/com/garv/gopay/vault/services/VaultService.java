package com.garv.gopay.vault.services;

import com.garv.gopay.payment.gateway.dto.PaymentRequest;
import com.garv.gopay.payment.processor.dto.PaymentProcessorResponse;
import com.garv.gopay.vault.dto.request.TokenizeCardRequest;
import com.garv.gopay.vault.dto.response.TokenizeCardResponse;
import lombok.NonNull;

import java.util.UUID;

public interface VaultService {

    TokenizeCardResponse tokenize(@NonNull TokenizeCardRequest request, @NonNull UUID merchantId);

    PaymentProcessorResponse charge(@NonNull String token, @NonNull PaymentRequest paymentRequest);

}
