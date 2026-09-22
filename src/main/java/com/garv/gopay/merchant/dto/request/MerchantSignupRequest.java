package com.garv.gopay.merchant.dto.request;

import com.garv.gopay.common.enums.BuisnessType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MerchantSignupRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Business name is required")
        @Size(max = 50, message = "Business name must be at most 50 characters")
        String businessName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotNull(message = "Business type is required")
        BuisnessType businessType
) {
}
