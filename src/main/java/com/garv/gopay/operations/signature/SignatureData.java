package com.garv.gopay.operations.signature;

import java.util.Map;
import java.util.UUID;

public record SignatureData(
        UUID eventId,
        long timestamp,
        Map<String, Object> payload
) {
}
