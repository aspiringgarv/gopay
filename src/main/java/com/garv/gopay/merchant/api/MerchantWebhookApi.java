package com.garv.gopay.merchant.api;

import com.garv.gopay.merchant.dto.response.WebhookConfigResponse;

import java.util.List;
import java.util.UUID;

public interface MerchantWebhookApi {

    List<WebhookConfigResponse> getActiveByEventType(UUID merchantId, String eventType);

}
