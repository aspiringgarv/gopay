package com.garv.gopay.vault.dto.request;

import com.garv.gopay.common.validation.LuhnCheck;
import com.garv.gopay.common.validation.NotPastYear;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TokenizeCardRequest(
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number must be 13 to 19 digits")
        @LuhnCheck(message = "Card number failed Luhn check")
        String pan,

        @NotBlank(message = "CVV is required")
        @Size(min = 3, max = 4, message = "CVV must be 3 or 4 digits")
        String cvv,

        @NotNull(message = "Expiry month is required")
        @Min(value = 1, message = "Expiry month must be between 1 and 12")
        @Max(value = 12, message = "Expiry month must be between 1 and 12")
        Integer expiryMonth,

        @NotNull(message = "Expiry year is required")
        @NotPastYear(message = "Expiry year cannot be in the past")
        Integer expiryYear,

        @NotNull(message = "Customer id is required")
        UUID customerId
) {
}
