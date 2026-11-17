package com.garv.gopay.merchant.dto.response;

public record WebhookTargetDto(
        String targetUrl,
        String eventTypes
) {
}
