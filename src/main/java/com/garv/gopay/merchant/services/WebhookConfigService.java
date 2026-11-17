package com.garv.gopay.merchant.services;

import com.garv.gopay.merchant.api.MerchantWebhookApi;
import com.garv.gopay.merchant.dto.request.UpdateWebhookConfigRequest;
import com.garv.gopay.merchant.dto.response.WebhookConfigResponse;

import java.util.List;
import java.util.UUID;

public interface WebhookConfigService extends MerchantWebhookApi {

    WebhookConfigResponse create(UUID merchantId, UpdateWebhookConfigRequest request);

    List<WebhookConfigResponse> getAll(UUID merchantId);

    @Override
    List<WebhookConfigResponse> getActiveByEventType(UUID merchantId, String eventType);

    WebhookConfigResponse getById(UUID merchantId, UUID id);

    WebhookConfigResponse update(UUID merchantId, UUID id, UpdateWebhookConfigRequest request);

    void delete(UUID merchantId, UUID id);

}
