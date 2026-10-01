package com.garv.gopay.merchant.dto.response;

import com.garv.gopay.common.enums.Environment;

import java.util.UUID;

public record ApikeyCreateResponse(
        UUID id,
        String keyId,
        String keySecret,
        Environment env
) {
}
