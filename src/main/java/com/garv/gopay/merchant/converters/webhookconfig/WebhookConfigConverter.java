package com.garv.gopay.merchant.converters.webhookconfig;

import com.garv.gopay.merchant.dto.request.UpdateWebhookConfigRequest;
import com.garv.gopay.merchant.dto.response.WebhookConfigResponse;
import com.garv.gopay.merchant.entities.Merchant;
import com.garv.gopay.merchant.entities.MerchantWebhookConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WebhookConfigConverter {

    @Mapping(target = "targetUrl", source = "request.targetUrl")
    @Mapping(target = "eventTypeFilter", source = "request.eventTypes")
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "id", ignore = true)
    MerchantWebhookConfig toEntity(Merchant merchant, UpdateWebhookConfigRequest request, byte[] encryptedSecret, byte[] encryptedDek);

    @Mapping(target = "eventTypes", source = "config.eventTypeFilter")
    @Mapping(target = "webhookSecret", ignore = true)
    WebhookConfigResponse toPojo(MerchantWebhookConfig config);

    List<WebhookConfigResponse> toPojos(List<MerchantWebhookConfig> configs);

    @Mapping(target = "eventTypes", source = "config.eventTypeFilter")
    WebhookConfigResponse toPojo(MerchantWebhookConfig config, String webhookSecret);

}
