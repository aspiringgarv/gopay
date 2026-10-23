package com.garv.gopay.payment.converters.payment;

import com.garv.gopay.payment.dto.request.PaymentInitRequest;
import com.garv.gopay.payment.dto.response.PaymentResponse;
import com.garv.gopay.payment.entities.OrderRecord;
import com.garv.gopay.payment.entities.Payment;
import com.garv.gopay.payment.gateway.dto.PaymentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentConverter {

    @Mapping(target = "orderId", source = "order.id")
    PaymentResponse toPojo(Payment payment);

    List<PaymentResponse> toPojos(List<Payment> payments);

    @Mapping(target = "order", source = "order")
    @Mapping(target = "merchantId", source = "merchantId")
    @Mapping(target = "idempotencyKey", source = "idempotencyKey")
    @Mapping(target = "amount", source = "order.amount")
    @Mapping(target = "method", source = "request.method")
    @Mapping(target = "methodDetails", source = "request.methodDetails")
    @Mapping(target = "status", constant = "CREATED")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bankReference", ignore = true)
    @Mapping(target = "errorCode", ignore = true)
    @Mapping(target = "errorDescription", ignore = true)
    @Mapping(target = "refundedAmount", expression = "java(com.garv.gopay.common.Money.of(0, order.getAmount().getCurrency()))")
    @Mapping(target = "authorizedAt", ignore = true)
    @Mapping(target = "capturedAt", ignore = true)
    @Mapping(target = "settledAt", ignore = true)
    Payment toEntity(PaymentInitRequest request, OrderRecord order, UUID merchantId, String idempotencyKey);

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "amount", source = "amount")
    PaymentRequest toGatewayRequest(Payment payment);

    @Mapping(target = "amount", ignore = true)
    void updatePaymentFromGatewayRequest(PaymentRequest gatewayRequest, @MappingTarget Payment payment);

}
