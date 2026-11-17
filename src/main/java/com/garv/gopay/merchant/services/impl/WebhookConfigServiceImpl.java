package com.garv.gopay.merchant.services.impl;

import com.garv.gopay.common.exceptions.ResourceNotFoundException;
import com.garv.gopay.common.util.RandomizerUtil;
import com.garv.gopay.common.config.WebhookEncryptionConfig;
import com.garv.gopay.merchant.converters.webhookconfig.WebhookConfigConverter;
import com.garv.gopay.merchant.dto.request.UpdateWebhookConfigRequest;
import com.garv.gopay.merchant.dto.response.WebhookConfigResponse;
import com.garv.gopay.merchant.entities.Merchant;
import com.garv.gopay.merchant.entities.MerchantWebhookConfig;
import com.garv.gopay.merchant.repositories.MerchantRepository;
import com.garv.gopay.merchant.repositories.WebhookConfigRepository;
import com.garv.gopay.merchant.services.WebhookConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookConfigServiceImpl implements WebhookConfigService {

    private final WebhookConfigRepository webhookConfigRepository;
    private final MerchantRepository merchantRepository;
    private final WebhookConfigConverter webhookConfigConverter;
    private final WebhookEncryptionConfig webhookEncryptionConfig;
    private final BytesEncryptor webhookMasterEncryptor;

    @Override
    @Transactional
    public WebhookConfigResponse create(UUID merchantId, UpdateWebhookConfigRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        String webhookSecret = RandomizerUtil.randomBase64(32);

        byte[] dek = webhookEncryptionConfig.webhookDekGenerator().generateKey();
        byte[] encryptedSecret = webhookEncryptionConfig.secretEncryptor(dek).encrypt(webhookSecret.getBytes(StandardCharsets.UTF_8));
        byte[] encryptedDek = webhookMasterEncryptor.encrypt(dek);

        MerchantWebhookConfig config = webhookConfigConverter.toEntity(merchant, request, encryptedSecret, encryptedDek);
        config = webhookConfigRepository.save(config);

        return webhookConfigConverter.toPojo(config, webhookSecret);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookConfigResponse> getAll(UUID merchantId) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new ResourceNotFoundException("Merchant", merchantId.toString());
        }
        return webhookConfigRepository.findByMerchant_Id(merchantId).stream()
                .map(config -> webhookConfigConverter.toPojo(config, decryptSecret(config)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookConfigResponse> getActiveByEventType(UUID merchantId, String eventType) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new ResourceNotFoundException("Merchant", merchantId.toString());
        }
        List<MerchantWebhookConfig> configs = webhookConfigRepository.findByMerchant_Id(merchantId).stream()
                .filter(MerchantWebhookConfig::isEnabled)
                .filter(config -> config.isSubscribed(eventType))
                .toList();
        return configs.stream()
                .map(config -> webhookConfigConverter.toPojo(config, decryptSecret(config)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WebhookConfigResponse getById(UUID merchantId, UUID id) {
        MerchantWebhookConfig config = webhookConfigRepository.findByIdAndMerchant_Id(id, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookConfig", id.toString()));
        return webhookConfigConverter.toPojo(config, decryptSecret(config));
    }

    @Override
    @Transactional
    public WebhookConfigResponse update(UUID merchantId, UUID id, UpdateWebhookConfigRequest request) {
        MerchantWebhookConfig config = webhookConfigRepository.findByIdAndMerchant_Id(id, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookConfig", id.toString()));

        config.setTargetUrl(request.targetUrl());
        config.setEventTypeFilter(request.eventTypes());
        config = webhookConfigRepository.save(config);

        return webhookConfigConverter.toPojo(config, decryptSecret(config));
    }

    @Override
    @Transactional
    public void delete(UUID merchantId, UUID id) {
        MerchantWebhookConfig config = webhookConfigRepository.findByIdAndMerchant_Id(id, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookConfig", id.toString()));
        webhookConfigRepository.delete(config);
    }

    private String decryptSecret(MerchantWebhookConfig config) {
        byte[] dek = webhookMasterEncryptor.decrypt(config.getEncryptedDek());
        byte[] secretBytes = webhookEncryptionConfig.secretEncryptor(dek).decrypt(config.getEncryptedSecret());
        try {
            return new String(secretBytes, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(dek, (byte) 0);
            Arrays.fill(secretBytes, (byte) 0);
        }
    }

}
