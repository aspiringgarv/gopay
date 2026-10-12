package com.garv.gopay.merchant.services.impl;

import com.garv.gopay.common.exceptions.DuplicateResourceException;
import com.garv.gopay.common.exceptions.InvalidCredentialsException;
import com.garv.gopay.merchant.config.JwtUtil;
import com.garv.gopay.merchant.converters.auth.AppUserAuthConverter;
import com.garv.gopay.merchant.converters.auth.MerchantAuthConverter;
import com.garv.gopay.merchant.dto.request.LoginRequest;
import com.garv.gopay.merchant.dto.request.RefreshTokenRequest;
import com.garv.gopay.merchant.dto.request.MerchantSignupRequest;
import com.garv.gopay.merchant.dto.response.LoginResponse;
import com.garv.gopay.merchant.dto.response.MerchantSignupResponse;
import com.garv.gopay.merchant.entities.AppUser;
import com.garv.gopay.merchant.entities.Merchant;
import com.garv.gopay.merchant.entities.RefreshToken;
import com.garv.gopay.merchant.repositories.AppUserRepository;
import com.garv.gopay.merchant.repositories.MerchantRepository;
import com.garv.gopay.merchant.repositories.RefreshTokenRepository;
import com.garv.gopay.merchant.services.AuthService;
import com.garv.gopay.common.util.RandomizerUtil;
import org.springframework.transaction.annotation.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final MerchantRepository merchantRepository;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MerchantAuthConverter merchantConverter;
    private final AppUserAuthConverter appUserConverter;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Override
    @Transactional
    public MerchantSignupResponse signup(@NonNull MerchantSignupRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("MERCHANT_WITH_THIS_EMAIL_ALREADY_EXISTS", "Email is already registered");
        }

        Merchant merchant = merchantConverter.toEntity(request);
        merchant = merchantRepository.save(merchant);

        AppUser owner = appUserConverter.toEntity(request, merchant);
        owner.setPasswordHash(passwordEncoder.encode(request.password()));
        appUserRepository.save(owner);

        return merchantConverter.toPojo(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(@NonNull LoginRequest request) {
        // same error for unknown email and wrong password so callers cannot tell which emails are registered
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException("INVALID_CREDENTIALS", "Invalid email or password");
        }
        AppUser user = (AppUser) authentication.getPrincipal();

        return issueTokens(user);
    }

    @Override
    @Transactional
    public LoginResponse refresh(@NonNull RefreshTokenRequest request) {
        RefreshToken stored = findAndVerify(request.refreshToken());

        if (stored.getRevokedAt() != null) {
            // this token was already rotated or logged out once before — someone is replaying it, kill the whole session
            log.warn("Reused refresh token presented for app user {}, revoking all its refresh tokens", stored.getAppUser().getId());
            refreshTokenRepository.revokeAllActiveForAppUser(stored.getAppUser().getId(), LocalDateTime.now());
            throw new InvalidCredentialsException("INVALID_REFRESH_TOKEN", "Refresh token has already been used");
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("INVALID_REFRESH_TOKEN", "Refresh token has expired");
        }

        stored.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getAppUser());
    }

    @Override
    @Transactional
    public void logout(@NonNull RefreshTokenRequest request) {
        // idempotent on purpose: a missing, malformed or already-revoked token still looks like a successful logout
        parseTokenId(request.refreshToken())
                .flatMap(refreshTokenRepository::findByTokenId)
                .filter(rt -> rt.getRevokedAt() == null)
                .ifPresent(rt -> {
                    rt.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(rt);
                });
    }

    private LoginResponse issueTokens(AppUser user) {
        UUID merchantId = user.getMerchant().getId();
        String accessToken = jwtUtil.generateToken(merchantId, user.getEmail(), user.getRole().name());
        String refreshToken = createRefreshToken(user);
        return new LoginResponse(accessToken, refreshToken, "Bearer", jwtUtil.getExpirationMs(), merchantId);
    }

    private String createRefreshToken(AppUser user) {
        String tokenId = RandomizerUtil.randomBase64(24);
        String secret = RandomizerUtil.randomBase64(40);

        RefreshToken refreshToken = RefreshToken.builder()
                .appUser(user)
                .tokenId(tokenId)
                .tokenSecretHash(passwordEncoder.encode(secret))
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L))
                .build();
        refreshTokenRepository.save(refreshToken);

        return tokenId + ":" + secret;
    }

    // same error for every failure shape (malformed, unknown id, wrong secret) so callers cannot probe token ids
    private RefreshToken findAndVerify(String rawToken) {
        int separator = rawToken.indexOf(':');
        if (separator <= 0 || separator == rawToken.length() - 1) {
            throw new InvalidCredentialsException("INVALID_REFRESH_TOKEN", "Invalid refresh token");
        }
        String tokenId = rawToken.substring(0, separator);
        String secret = rawToken.substring(separator + 1);

        return refreshTokenRepository.findByTokenId(tokenId)
                .filter(rt -> passwordEncoder.matches(secret, rt.getTokenSecretHash()))
                .orElseThrow(() -> new InvalidCredentialsException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));
    }

    private Optional<String> parseTokenId(String rawToken) {
        int separator = rawToken.indexOf(':');
        if (separator <= 0 || separator == rawToken.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(rawToken.substring(0, separator));
    }

}
