package com.garv.gopay.vault.converters.vaultcard;

import com.garv.gopay.common.enums.CardBrand;
import com.garv.gopay.vault.dto.request.TokenizeCardRequest;
import com.garv.gopay.vault.entities.VaultCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VaultCardConverter {

    @Mapping(target = "encryptedPan", source = "encryptedPan")
    @Mapping(target = "encryptedDek", source = "encryptedDek")
    @Mapping(target = "lastFour", source = "lastFour")
    @Mapping(target = "brand", source = "brand")
    @Mapping(target = "bin", source = "bin")
    @Mapping(target = "expiryMonth", source = "request.expiryMonth")
    @Mapping(target = "expiryYear", source = "request.expiryYear")
    @Mapping(target = "id", ignore = true)
    VaultCard toEntity(byte[] encryptedPan, byte[] encryptedDek, String lastFour, CardBrand brand, String bin, TokenizeCardRequest request);

}
