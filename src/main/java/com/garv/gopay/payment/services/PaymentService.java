package com.garv.gopay.payment.services;

import com.garv.gopay.payment.dto.request.PaymentInitRequest;
import com.garv.gopay.payment.dto.response.PaymentResponse;
import lombok.NonNull;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    List<PaymentResponse> listByOrder(@NonNull UUID orderId, @NonNull UUID merchantId);

    List<PaymentResponse> listByMerchant(@NonNull UUID merchantId);

    PaymentResponse initiate(@NonNull PaymentInitRequest paymentInitRequest, @NonNull UUID merchantId);

    PaymentResponse capture(@NonNull UUID paymentId, @NonNull UUID merchantId);

    void resolveAuthorization(@NonNull UUID paymentId, boolean approve, String bankRef, String errorCode, String errorDesc);

}
