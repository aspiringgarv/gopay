package com.garv.gopay.payment.services;

import com.garv.gopay.payment.dto.response.PaymentResponse;
import lombok.NonNull;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    List<PaymentResponse> listByOrder(@NonNull UUID orderId, @NonNull UUID merchantId);

}
