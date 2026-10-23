package com.garv.gopay.payment.converters.order;

import com.garv.gopay.payment.dto.request.CreateOrderRequest;
import com.garv.gopay.payment.dto.response.OrderResponse;
import com.garv.gopay.payment.entities.OrderRecord;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderConverter {

    @Mapping(target = "status", constant = "CREATED")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "attempts", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    OrderRecord toEntity(CreateOrderRequest request, UUID merchantId, @Context int defaultExpiryMinutes);

    OrderResponse toPojo(OrderRecord orderRecord);

    @AfterMapping
    default void applyDefaults(CreateOrderRequest request, @MappingTarget OrderRecord orderRecord, @Context int defaultExpiryMinutes) {
        orderRecord.setExpiresAt(Objects.nonNull(request.expiresAt()) ? request.expiresAt() : LocalDateTime.now().plusMinutes(defaultExpiryMinutes));
        orderRecord.setCreatedAt(LocalDateTime.now());
    }

}
