package com.garv.gopay.merchant.services;

import com.garv.gopay.merchant.dto.request.MerchantSignupRequest;
import com.garv.gopay.merchant.dto.response.MerchantSignupResponse;
import org.jspecify.annotations.NonNull;

public interface AuthService {

    MerchantSignupResponse signup(@NonNull MerchantSignupRequest request);

}
