package com.garv.gopay.merchant.converters.apikey;

import com.garv.gopay.merchant.dto.request.CreateApiKeyRequest;
import com.garv.gopay.merchant.dto.response.ApiKeyResponse;
import com.garv.gopay.merchant.dto.response.ApikeyCreateResponse;
import com.garv.gopay.merchant.entities.ApiKey;
import com.garv.gopay.merchant.entities.Merchant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApiKeyConverter {

    @Mapping(target = "environment", source = "request.environment")
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "previousKeySecretHash", ignore = true)
    @Mapping(target = "webhookSecretHash", ignore = true)
    @Mapping(target = "lastUsedAt", ignore = true)
    @Mapping(target = "rotatedAt", ignore = true)
    @Mapping(target = "gracePeriodExpiresAt", ignore = true)
    ApiKey toEntity(Merchant merchant, CreateApiKeyRequest request, String keyId, String keySecretHash);

    ApiKeyResponse toPojo(ApiKey apiKey);

    List<ApiKeyResponse> toPojos(List<ApiKey> apiKeys);

    @Mapping(target = "env", source = "apiKey.environment")
    ApikeyCreateResponse toPojo(ApiKey apiKey, String keySecret);

}
