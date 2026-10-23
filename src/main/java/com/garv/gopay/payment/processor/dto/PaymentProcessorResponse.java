package com.garv.gopay.payment.processor.dto;

public sealed interface PaymentProcessorResponse
        permits PaymentProcessorResponse.Pending, PaymentProcessorResponse.Success, PaymentProcessorResponse.Failure {

    record Pending(String processorRef) implements PaymentProcessorResponse {
    }

    record Success(String bankRef, String processorRef) implements PaymentProcessorResponse {
    }

    record Failure(String errorCode, String errorDescription) implements PaymentProcessorResponse {
    }

}
