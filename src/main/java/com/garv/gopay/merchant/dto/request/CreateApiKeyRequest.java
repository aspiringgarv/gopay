package com.garv.gopay.merchant.dto.request;

import com.garv.gopay.common.enums.Environment;
import jakarta.validation.constraints.NotNull;

public record CreateApiKeyRequest(
        @NotNull(message = "Environment is required")
        Environment environment
) {
}
