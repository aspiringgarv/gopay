package com.garv.gopay.merchant.dto.response;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UUID merchantId
) {
}
