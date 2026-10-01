package com.garv.gopay.payment.services.impl;

import com.garv.gopay.common.enums.OrderStatus;
import com.garv.gopay.common.exceptions.DuplicateResourceException;
import com.garv.gopay.common.exceptions.BusinessRuleViolationException;
import com.garv.gopay.common.exceptions.ResourceNotFoundException;
import com.garv.gopay.payment.converters.order.OrderConverter;
import com.garv.gopay.payment.dto.request.CreateOrderRequest;
import com.garv.gopay.payment.dto.response.OrderResponse;
import com.garv.gopay.payment.entities.OrderRecord;
import com.garv.gopay.payment.repositories.OrderRepository;
import com.garv.gopay.payment.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderConverter orderConverter;

    @Value("${payment.order.default-expiry-minutes:30}")
    private int defaultExpiryMinutes;

    @Override
    @Transactional
    public OrderResponse create(CreateOrderRequest request , UUID merchantId) {
        log.info("Creating order for merchantId: {} with request: {}", merchantId, request);
        if(Objects.nonNull(request.receipt()) && orderRepository.existsByMerchantIdAndReceipt(merchantId, request.receipt())){
            throw new DuplicateResourceException("ORDER_RECIEPT_DUPLICATE", "Order with the same receipt already exists for this merchant.");
        }
        // TODO send Kafka event for order creation
        OrderRecord orderRecord = orderConverter.toEntity(request, merchantId, defaultExpiryMinutes);
        orderRecord = orderRepository.save(orderRecord);
        return orderConverter.toPojo(orderRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(UUID orderId, UUID merchantId) {
        OrderRecord orderRecord = orderRepository.findByIdAndMerchantId(orderId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        return orderConverter.toPojo(orderRecord);
    }

    @Override
    @Transactional
    public OrderResponse cancel(UUID orderId, UUID merchantId) {
        OrderRecord orderRecord = orderRepository.findByIdAndMerchantId(orderId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (orderRecord.getStatus() == OrderStatus.PAID) {
            throw new BusinessRuleViolationException("ORDER_ALREADY_PAID", "Cannot cancel an order that has already been paid.");
        }
        if (orderRecord.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException("ORDER_ALREADY_CANCELLED", "Order is already cancelled.");
        }

        orderRecord.setStatus(OrderStatus.CANCELLED);
        orderRecord = orderRepository.save(orderRecord);
        return orderConverter.toPojo(orderRecord);
    }

}
