package com.garv.gopay.payment.gateway.dto;

public sealed interface PaymentResult
        permits PaymentResult.Pending, PaymentResult.Success, PaymentResult.Failure {

    record Pending(String registrationRef) implements PaymentResult {
    }

    record Success(String bankReference) implements PaymentResult {
    }

    record Failure(String errorCode, String errorDescription) implements PaymentResult {
    }

}
