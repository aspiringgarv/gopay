package com.garv.gopay.merchant.dto.response;

import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

public record WebhookConfigResponse(
        UUID id,
        String targetUrl,
        String eventTypes,
        boolean enabled,
        @Nullable String webhookSecret,
        LocalDateTime createdAt
) {
}
