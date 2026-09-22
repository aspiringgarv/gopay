package com.garv.gopay.merchant.converters.auth;

import com.garv.gopay.common.enums.UserRole;
import com.garv.gopay.merchant.dto.request.MerchantSignupRequest;
import com.garv.gopay.merchant.dto.response.AppUserResponse;
import com.garv.gopay.merchant.entities.AppUser;
import com.garv.gopay.merchant.entities.Merchant;
import org.springframework.stereotype.Component;

@Component
public class AppUserAuthConverter {

    public AppUser toEntity(MerchantSignupRequest request, Merchant merchant) {
        return AppUser.builder()
                .merchant(merchant)
                .email(request.email())
                .passwordHash(request.password()) // TODO: Hash the password before storing it via Bcrypt
                .role(UserRole.OWNER)
                .build();
    }

    public AppUserResponse toPojo(AppUser appUser) {
        return new AppUserResponse(
                appUser.getId(),
                appUser.getEmail(),
                appUser.getRole(),
                appUser.getCreatedAt()
        );
    }

}
