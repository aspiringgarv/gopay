package com.garv.gopay.merchant.dto.response;

import com.garv.gopay.common.enums.BuisnessType;
import com.garv.gopay.common.enums.MerchantStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record MerchantSignupResponse(
        UUID id,
        String name,
        String businessName,
        String email,
        BuisnessType businessType,
        MerchantStatus status,
        LocalDateTime createdAt
) {
}
