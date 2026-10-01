package com.garv.gopay.payment.services.impl;

import com.garv.gopay.common.exceptions.ResourceNotFoundException;
import com.garv.gopay.payment.converters.payment.PaymentConverter;
import com.garv.gopay.payment.dto.response.PaymentResponse;
import com.garv.gopay.payment.repositories.OrderRepository;
import com.garv.gopay.payment.repositories.PaymentRepository;
import com.garv.gopay.payment.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentConverter paymentConverter;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> listByOrder(UUID orderId, UUID merchantId) {
        if (!orderRepository.existsByIdAndMerchantId(orderId, merchantId)) {
            throw new ResourceNotFoundException("Order_NOT_FOUND_FOR", orderId.toString());
        }

        return paymentConverter.toPojos(paymentRepository.findByOrder_IdAndMerchantId(orderId, merchantId));
    }

}
