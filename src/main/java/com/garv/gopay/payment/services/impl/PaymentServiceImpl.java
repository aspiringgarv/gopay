package com.garv.gopay.payment.services.impl;

import com.garv.gopay.common.enums.Actor;
import com.garv.gopay.common.enums.OrderStatus;
import com.garv.gopay.common.enums.PaymentEvent;
import com.garv.gopay.common.enums.PaymentStatus;
import com.garv.gopay.common.exceptions.BusinessRuleViolationException;
import com.garv.gopay.common.exceptions.ResourceNotFoundException;
import com.garv.gopay.common.util.RandomizerUtil;
import com.garv.gopay.payment.converters.payment.PaymentConverter;
import com.garv.gopay.payment.dto.request.PaymentInitRequest;
import com.garv.gopay.payment.dto.response.PaymentResponse;
import com.garv.gopay.payment.entities.OrderRecord;
import com.garv.gopay.payment.entities.Payment;
import com.garv.gopay.payment.gateway.PaymentGatewayRouter;
import com.garv.gopay.payment.gateway.dto.PaymentRequest;
import com.garv.gopay.payment.gateway.dto.PaymentResult;
import com.garv.gopay.payment.repositories.OrderRepository;
import com.garv.gopay.payment.repositories.PaymentRepository;
import com.garv.gopay.payment.services.PaymentService;
import com.garv.gopay.payment.services.PaymentTransitionService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentConverter paymentConverter;
    private final PaymentGatewayRouter paymentGatewayRouter;
    private final PaymentTransitionService paymentTransitionService;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> listByOrder(@NonNull UUID orderId, @NonNull UUID merchantId) {
        if (!orderRepository.existsByIdAndMerchantId(orderId, merchantId)) {
            throw new ResourceNotFoundException("Order_NOT_FOUND_FOR", orderId.toString());
        }

        return paymentConverter.toPojos(paymentRepository.findByOrder_IdAndMerchantId(orderId, merchantId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> listByMerchant(@NonNull UUID merchantId) {
        return paymentConverter.toPojos(paymentRepository.findByMerchantId(merchantId));
    }

    @Override
    @Transactional
    public PaymentResponse initiate(@NonNull PaymentInitRequest paymentInitRequest, @NonNull UUID merchantId) {
        // locked for the rest of this transaction: two concurrent initiate() calls on the same order
        // could otherwise both read status != PAID/CANCELLED, both pass the checks below, and both
        // create a payment — the lock makes the second request wait until this one commits instead
        OrderRecord order = orderRepository.findByIdAndMerchantIdForUpdate(paymentInitRequest.orderId(), merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", paymentInitRequest.orderId().toString()));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessRuleViolationException("ORDER_ALREADY_PAID", "Cannot initiate payment for an order that has already been paid.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException("ORDER_CANCELLED", "Cannot initiate payment for a cancelled order.");
        }

        String idempotencyKey = RandomizerUtil.randomBase64(40);
        Payment payment = paymentConverter.toEntity(paymentInitRequest, order, merchantId, idempotencyKey);
        payment = paymentRepository.save(payment);

        PaymentRequest gatewayRequest = paymentConverter.toGatewayRequest(payment);

        PaymentResult gatewayResult;
        try {
            gatewayResult = paymentGatewayRouter.initiate(gatewayRequest);
            log.info("Gateway result for paymentId {}: {}", payment.getId(), gatewayResult);
        } catch (Exception ex) {
            log.error("Gateway call failed for paymentId {}", payment.getId(), ex);
            gatewayResult = new PaymentResult.Failure("GATEWAY_ERROR", ex.getMessage());
        }

        switch (gatewayResult) {
            case PaymentResult.Pending pending -> {
                paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_ATTEMPT, Actor.SYSTEM, "Gateway returned pending authorization");
                payment.setProcessorReference(pending.registrationRef());
            }
            case PaymentResult.Failure failure -> {
                paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_FAIL, Actor.SYSTEM, failure.errorDescription());
                payment.setErrorCode(failure.errorCode());
                payment.setErrorDescription(failure.errorDescription());
            }
            default -> throw new IllegalStateException("Gateway initiate should never return " + gatewayResult.getClass().getSimpleName());
        }

        order.setStatus(OrderStatus.ATTEMPTED);
        order.setAttempts(order.getAttempts() + 1);
        orderRepository.save(order);

        payment = paymentRepository.save(payment);

        //TODO : Kafka event publishing for payment initiation

        return paymentConverter.toPojo(payment);
    }

    @Override
    @Transactional
    public PaymentResponse capture(@NonNull UUID paymentId, @NonNull UUID merchantId) {
        // locked for the rest of this transaction: two concurrent capture() calls on the same payment
        // could otherwise both read AUTHORIZED, both pass the check below, and both capture it —
        // the lock makes the second request wait until this one commits instead
        Payment payment = paymentRepository.findByIdAndMerchantIdForUpdate(paymentId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId.toString()));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new BusinessRuleViolationException("PAYMENT_NOT_AUTHORIZED", "Cannot capture a payment that is not in AUTHORIZED state.");
        }

        payment = captureAuthorized(payment, Actor.MERCHANT, "Merchant requested capture");

        return paymentConverter.toPojo(payment);
    }

    @Override
    @Transactional
    public void resolveAuthorization(@NonNull UUID paymentId, boolean approve, String bankRef, String errorCode, String errorDesc) {
        // locked for the rest of this transaction: an overlapping bank callback (or a manual capture
        // racing it) on the same payment could otherwise both read AUTHORIZING and both act on it —
        // the lock makes the second one wait until this one commits instead
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId.toString()));

        if (payment.getStatus() != PaymentStatus.AUTHORIZING) {
            log.warn("Ignoring bank callback for paymentId {}: expected AUTHORIZING but status is {}", paymentId, payment.getStatus());
            return;
        }

        if (approve) {
            paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_SUCCESS, Actor.SYSTEM, "Bank callback authorized the payment");
            payment.setBankReference(bankRef);
            payment.setAuthorizedAt(LocalDateTime.now());
            captureAuthorized(payment, Actor.SYSTEM, "Auto-capture after bank authorization");
            return;
        }

        paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_FAIL, Actor.SYSTEM, errorDesc);
        payment.setErrorCode(errorCode);
        payment.setErrorDescription(errorDesc);
        paymentRepository.save(payment);
    }

    private Payment captureAuthorized(Payment payment, Actor requestedBy, String reason) {
        paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_REQUEST, requestedBy, reason);
        payment = paymentRepository.save(payment);

        PaymentRequest gatewayRequest = paymentConverter.toGatewayRequest(payment);

        PaymentResult gatewayResult;
        try {
            gatewayResult = paymentGatewayRouter.capture(gatewayRequest);
            log.info("Capture gateway result for paymentId {}: {}", payment.getId(), gatewayResult);
        } catch (Exception ex) {
            log.error("Capture gateway call failed for paymentId {}", payment.getId(), ex);
            gatewayResult = new PaymentResult.Failure("GATEWAY_ERROR", ex.getMessage());
        }

        switch (gatewayResult) {
            case PaymentResult.Pending pending -> payment.setProcessorReference(pending.registrationRef());
            case PaymentResult.Success success -> {
                paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_SUCCESS, Actor.SYSTEM, "Gateway captured the payment");
                payment.setCapturedAt(LocalDateTime.now());
                if (success.bankReference() != null) {
                    payment.setBankReference(success.bankReference());
                }
            }
            case PaymentResult.Failure failure -> {
                paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_FAIL, Actor.SYSTEM, failure.errorDescription());
                payment.setErrorCode(failure.errorCode());
                payment.setErrorDescription(failure.errorDescription());
            }
        }

        payment = paymentRepository.save(payment);

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            OrderRecord order = payment.getOrder();
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
        }

        return payment;
    }

}
