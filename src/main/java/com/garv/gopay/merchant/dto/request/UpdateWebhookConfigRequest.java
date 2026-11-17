package com.garv.gopay.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWebhookConfigRequest(
        @NotBlank(message = "Target URL is required")
        @Size(max = 2048, message = "Target URL must be at most 2048 characters")
        @Pattern(regexp = "^https?://.+", message = "Target URL must be a valid http(s) URL")
        String targetUrl,

        @NotBlank(message = "Event types are required")
        @Size(max = 255, message = "Event types must be at most 255 characters")
        @Pattern(regexp = "^[A-Z]+(_[A-Z]+)*(,[A-Z]+(_[A-Z]+)*)*$", message = "Event types must be a comma-separated list of upper snake case values, e.g. PAYMENT_STATUS_CHANGED,ORDER_CREATED")
        String eventTypes
) {
}
