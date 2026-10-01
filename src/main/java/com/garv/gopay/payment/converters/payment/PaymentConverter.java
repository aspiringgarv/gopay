package com.garv.gopay.payment.converters.payment;

import com.garv.gopay.payment.dto.response.PaymentResponse;
import com.garv.gopay.payment.entities.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentConverter {

    @Mapping(target = "orderId", source = "order.id")
    PaymentResponse toPojo(Payment payment);

    List<PaymentResponse> toPojos(List<Payment> payments);

}
