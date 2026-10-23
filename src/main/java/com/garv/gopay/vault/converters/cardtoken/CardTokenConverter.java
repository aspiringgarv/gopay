package com.garv.gopay.vault.converters.cardtoken;

import com.garv.gopay.vault.entities.CardToken;
import com.garv.gopay.vault.entities.VaultCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CardTokenConverter {

    @Mapping(target = "token", source = "token")
    @Mapping(target = "vaultCard", source = "vaultCard")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "merchantId", source = "merchantId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "revokedAt", ignore = true)
    CardToken toEntity(String token, VaultCard vaultCard, UUID customerId, UUID merchantId);

}
