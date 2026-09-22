package com.garv.gopay.merchant.converters.auth;

import com.garv.gopay.merchant.dto.request.MerchantSignupRequest;
import com.garv.gopay.merchant.dto.response.MerchantSignupResponse;
import com.garv.gopay.merchant.entities.Merchant;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class MerchantAuthConverter {

    public Merchant toEntity(MerchantSignupRequest request) {
        return Merchant.builder()
                .name(request.name())
                .businessName(request.businessName())
                .email(request.email())
                .buisnessType(request.businessType())
                .merchantId(UUID.randomUUID().toString())
                .build();
    }

    public MerchantSignupResponse toPojo(Merchant merchant) {
        return new MerchantSignupResponse(
                merchant.getId(),
                merchant.getName(),
                merchant.getBusinessName(),
                merchant.getEmail(),
                merchant.getBuisnessType(),
                merchant.getMerchantStatus(),
                LocalDateTime.now()
        );
    }

}
