package com.garv.gopay.payment.controllers;

import com.garv.gopay.merchant.context.MerchantContext;
import com.garv.gopay.payment.dto.request.CreateOrderRequest;
import com.garv.gopay.payment.dto.response.OrderResponse;
import com.garv.gopay.payment.dto.response.PaymentResponse;
import com.garv.gopay.payment.services.OrderService;
import com.garv.gopay.payment.services.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    private final MerchantContext merchantContext;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request, merchantContext.requireMerchantId()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getById(@PathVariable UUID orderId) {
        // so that any other merchant cannot access the order of another merchant
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getById(orderId, merchantContext.requireMerchantId()));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable UUID orderId) {
        return ResponseEntity.status(HttpStatus.OK).body(orderService.cancel(orderId, merchantContext.requireMerchantId()));
    }

    @GetMapping("/{orderId}/payments")
    public ResponseEntity<List<PaymentResponse>> listPayments(@PathVariable UUID orderId) {
        return ResponseEntity.status(HttpStatus.OK).body(paymentService.listByOrder(orderId, merchantContext.requireMerchantId()));
    }

}
