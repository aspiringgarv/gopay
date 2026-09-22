package com.garv.gopay.merchant.dto.response;

import com.garv.gopay.common.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppUserResponse(
        UUID id,
        String email,
        UserRole role,
        LocalDateTime createdAt
) {
}
