package com.garv.gopay.merchant.services;

import com.garv.gopay.merchant.dto.request.CreateApiKeyRequest;
import com.garv.gopay.merchant.dto.response.ApiKeyResponse;
import com.garv.gopay.merchant.dto.response.ApikeyCreateResponse;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface ApiKeyService {
    @Nullable ApikeyCreateResponse create(UUID merchantId, CreateApiKeyRequest request);

    List<ApiKeyResponse> getAll(UUID merchantId);

    void revoke(UUID merchantId, String keyId);

    ApikeyCreateResponse rotate(UUID merchantId, String keyId);

    /** Verifies a key id and secret and returns the id of the merchant that owns the key. */
    UUID authenticate(String keyId, String keySecret);
}
