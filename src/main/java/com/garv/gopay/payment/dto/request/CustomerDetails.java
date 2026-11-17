package com.garv.gopay.payment.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerDetails(
        @Size(max = 200, message = "Customer name must have at most 200 characters")
        String name,

        @NotBlank(message = "Customer email is required")
        @Email(message = "Customer email must be a valid email address")
        String email,

        @Size(max = 20, message = "Customer phone must have at most 20 characters")
        String phone
) {
    // CreateOrderRequest is logged as a whole, so keep customer PII out of toString
    @Override
    public String toString() {
        return "CustomerDetails[redacted]";
    }
}
