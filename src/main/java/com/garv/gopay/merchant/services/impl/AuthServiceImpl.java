package com.garv.gopay.merchant.services.impl;

import com.garv.gopay.merchant.converters.auth.AppUserAuthConverter;
import com.garv.gopay.merchant.converters.auth.MerchantAuthConverter;
import com.garv.gopay.merchant.dto.request.MerchantSignupRequest;
import com.garv.gopay.merchant.dto.response.MerchantSignupResponse;
import com.garv.gopay.merchant.entities.AppUser;
import com.garv.gopay.merchant.entities.Merchant;
import com.garv.gopay.merchant.repositories.AppUserRepository;
import com.garv.gopay.merchant.repositories.MerchantRepository;
import com.garv.gopay.merchant.services.AuthService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final MerchantRepository merchantRepository;
    private final AppUserRepository appUserRepository;
    private final MerchantAuthConverter merchantConverter;
    private final AppUserAuthConverter appUserConverter;

    @Override
    public MerchantSignupResponse signup(@NonNull MerchantSignupRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Merchant merchant = merchantConverter.toEntity(request);
        merchant = merchantRepository.save(merchant);

        AppUser owner = appUserConverter.toEntity(request, merchant);
        appUserRepository.save(owner);

        return merchantConverter.toPojo(merchant);
    }

}
