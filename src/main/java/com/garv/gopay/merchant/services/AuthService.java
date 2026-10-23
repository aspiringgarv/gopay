package com.garv.gopay.merchant.services;

import com.garv.gopay.merchant.dto.request.LoginRequest;
import com.garv.gopay.merchant.dto.request.RefreshTokenRequest;
import com.garv.gopay.merchant.dto.request.MerchantSignupRequest;
import com.garv.gopay.merchant.dto.response.LoginResponse;
import com.garv.gopay.merchant.dto.response.MerchantSignupResponse;
import org.jspecify.annotations.NonNull;

public interface AuthService {

    MerchantSignupResponse signup(@NonNull MerchantSignupRequest request);

    LoginResponse login(@NonNull LoginRequest request);

    /** Exchanges a valid, unused refresh token for a new access token and rotates the refresh token. */
    LoginResponse refresh(@NonNull RefreshTokenRequest request);

    /** Revokes a refresh token. Always succeeds, even if the token is unknown or already revoked. */
    void logout(@NonNull RefreshTokenRequest request);

}
