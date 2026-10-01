package com.garv.gopay.payment.services;

import com.garv.gopay.payment.dto.request.CreateOrderRequest;
import com.garv.gopay.payment.dto.response.OrderResponse;
import lombok.NonNull;

import java.util.UUID;

public interface OrderService {

    OrderResponse create(@NonNull CreateOrderRequest request, @NonNull UUID merchantId);

    OrderResponse getById(@NonNull UUID orderId, @NonNull UUID merchantId);

    OrderResponse cancel(@NonNull UUID orderId, @NonNull UUID merchantId);

}
