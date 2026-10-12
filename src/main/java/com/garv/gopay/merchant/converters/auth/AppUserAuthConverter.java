package com.garv.gopay.merchant.converters.auth;

import com.garv.gopay.merchant.dto.request.MerchantSignupRequest;
import com.garv.gopay.merchant.dto.response.AppUserResponse;
import com.garv.gopay.merchant.entities.AppUser;
import com.garv.gopay.merchant.entities.Merchant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AppUserAuthConverter {

    @Mapping(target = "email", source = "request.email")
    @Mapping(target = "passwordHash", source = "request.password") // hashed with BCrypt in AuthServiceImpl.signup right after mapping
    @Mapping(target = "role", constant = "OWNER")
    @Mapping(target = "id", ignore = true)
    AppUser toEntity(MerchantSignupRequest request, Merchant merchant);

    AppUserResponse toPojo(AppUser appUser);

}
