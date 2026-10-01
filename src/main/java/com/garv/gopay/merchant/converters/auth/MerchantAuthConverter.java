package com.garv.gopay.merchant.converters.auth;

import com.garv.gopay.merchant.dto.request.MerchantSignupRequest;
import com.garv.gopay.merchant.dto.response.MerchantSignupResponse;
import com.garv.gopay.merchant.entities.Merchant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MerchantAuthConverter {

    @Mapping(target = "buisnessType", source = "businessType")
    @Mapping(target = "merchantId", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "merchantStatus", ignore = true)
    @Mapping(target = "contactNumber", ignore = true)
    @Mapping(target = "gstId", ignore = true)
    @Mapping(target = "pan", ignore = true)
    @Mapping(target = "settlementBankAccount", ignore = true)
    @Mapping(target = "settlementIfsc", ignore = true)
    @Mapping(target = "settlementAccountHolderName", ignore = true)
    Merchant toEntity(MerchantSignupRequest request);

    @Mapping(target = "businessType", source = "buisnessType")
    @Mapping(target = "status", source = "merchantStatus")
    @Mapping(target = "createdAt", ignore = true)
    MerchantSignupResponse toPojo(Merchant merchant);

}
