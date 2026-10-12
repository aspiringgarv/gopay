package com.garv.gopay.payment.controllers;

import com.garv.gopay.merchant.context.MerchantContext;
import com.garv.gopay.payment.dto.request.PaymentInitRequest;
import com.garv.gopay.payment.dto.response.PaymentResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    private final MerchantContext merchantContext;

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(paymentService.listByMerchant(merchantContext.requireMerchantId()));
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> initiate(@Valid @RequestBody PaymentInitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initiate(request, merchantContext.requireMerchantId()));
    }

    @PutMapping("/{paymentId}/capture")
    public ResponseEntity<PaymentResponse> capture(@PathVariable UUID paymentId) {
        return ResponseEntity.status(HttpStatus.OK).body(paymentService.capture(paymentId, merchantContext.requireMerchantId()));
    }

}
