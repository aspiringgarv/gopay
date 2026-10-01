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
    @Mapping(target = "passwordHash", source = "request.password") // TODO: Hash the password before storing it via Bcrypt
    @Mapping(target = "role", constant = "OWNER")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AppUser toEntity(MerchantSignupRequest request, Merchant merchant);

    AppUserResponse toPojo(AppUser appUser);

}
