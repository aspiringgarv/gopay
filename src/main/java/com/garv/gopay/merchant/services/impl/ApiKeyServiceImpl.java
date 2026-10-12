package com.garv.gopay.merchant.services.impl;

import com.garv.gopay.common.exceptions.ApiKeyRevokedException;
import com.garv.gopay.common.exceptions.InvalidCredentialsException;
import com.garv.gopay.common.exceptions.ResourceNotFoundException;
import com.garv.gopay.common.util.RandomizerUtil;
import com.garv.gopay.merchant.converters.apikey.ApiKeyConverter;
import com.garv.gopay.merchant.dto.request.CreateApiKeyRequest;
import com.garv.gopay.merchant.dto.response.ApiKeyResponse;
import com.garv.gopay.merchant.dto.response.ApikeyCreateResponse;
import com.garv.gopay.merchant.entities.ApiKey;
import com.garv.gopay.merchant.entities.Merchant;
import com.garv.gopay.merchant.repositories.ApiKeyRepository;
import com.garv.gopay.merchant.repositories.MerchantRepository;
import com.garv.gopay.merchant.services.ApiKeyService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final MerchantRepository merchantRepository;
    private final ApiKeyConverter apiKeyConverter;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public @Nullable ApikeyCreateResponse create(UUID merchantId, CreateApiKeyRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        String keyId = "gopay_" + request.environment().name().toUpperCase() +"_"+ RandomizerUtil.randomBase64(24);
        String keySecret = RandomizerUtil.randomBase64(40);

        ApiKey apiKey = apiKeyConverter.toEntity(merchant, request, keyId, passwordEncoder.encode(keySecret));

        apiKey = apiKeyRepository.save(apiKey);

        return apiKeyConverter.toPojo(apiKey, keySecret);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getAll(UUID merchantId) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new ResourceNotFoundException("Merchant", merchantId.toString());
        }

        return apiKeyConverter.toPojos(apiKeyRepository.findByMerchant_Id(merchantId));
    }

    @Override
    @Transactional
    public void revoke(UUID merchantId, String keyId) {
        ApiKey apiKey = apiKeyRepository.findByKeyIdAndMerchant_Id(keyId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", keyId));

        apiKey.setEnabled(false);
        apiKeyRepository.save(apiKey);
    }

    @Override
    @Transactional
    public ApikeyCreateResponse rotate(UUID merchantId, String keyId) {
        ApiKey apiKey = apiKeyRepository.findByKeyIdAndMerchant_Id(keyId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", keyId));

        if (!apiKey.isEnabled()) {
            throw new ApiKeyRevokedException("API_KEY_REVOKED", "Cannot rotate a revoked API key: " + keyId);
        }

        String newKeySecret = RandomizerUtil.randomBase64(40);

        apiKey.setPreviousKeySecretHash(apiKey.getKeySecretHash());
        apiKey.setKeySecretHash(passwordEncoder.encode(newKeySecret));
        apiKey.setRotatedAt(LocalDateTime.now());
        apiKey.setGracePeriodExpiresAt(LocalDateTime.now().plusDays(1));

        apiKey = apiKeyRepository.save(apiKey);

        return apiKeyConverter.toPojo(apiKey, newKeySecret);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID authenticate(String keyId, String keySecret) {
        // same error for unknown key, revoked key and wrong secret so callers cannot probe which key ids exist
        ApiKey apiKey = apiKeyRepository.findByKeyId(keyId)
                .filter(ApiKey::isEnabled)
                .filter(key -> secretMatches(key, keySecret))
                .orElseThrow(() -> new InvalidCredentialsException("INVALID_API_KEY", "Invalid API key or secret"));

        return apiKey.getMerchant().getId();
    }

    private boolean secretMatches(ApiKey apiKey, String keySecret) {
        if (passwordEncoder.matches(keySecret, apiKey.getKeySecretHash())) {
            return true;
        }
        // after a rotation the previous secret keeps working until the grace period ends
        return apiKey.getPreviousKeySecretHash() != null
                && apiKey.getGracePeriodExpiresAt() != null
                && LocalDateTime.now().isBefore(apiKey.getGracePeriodExpiresAt())
                && passwordEncoder.matches(keySecret, apiKey.getPreviousKeySecretHash());
    }
}
