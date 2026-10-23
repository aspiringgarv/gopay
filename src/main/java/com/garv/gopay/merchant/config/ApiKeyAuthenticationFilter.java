package com.garv.gopay.merchant.config;

import com.garv.gopay.common.exceptions.BadRequestException;
import com.garv.gopay.common.exceptions.InvalidCredentialsException;
import com.garv.gopay.common.exceptions.RateLimitExceededException;
import com.garv.gopay.common.ratelimiting.cache.ApiKeyRateLimitPolicy;
import com.garv.gopay.common.ratelimiting.RateLimiter;
import com.garv.gopay.common.ratelimiting.RatelimitResult;
import com.garv.gopay.common.ratelimiting.cache.ApiKeyCache;
import com.garv.gopay.common.ratelimiting.cache.ApiKeyCacheEntry;
import com.garv.gopay.merchant.context.MerchantContext;
import com.garv.gopay.merchant.entities.ApiKey;
import com.garv.gopay.merchant.repositories.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// authenticates merchant API calls with "Authorization: Basic base64(keyId:keySecret)"
// not a @Component on purpose: it is created in WebSecurityConfig so it only runs inside the API key filter chain
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String BASIC_PREFIX = "Basic ";

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyCache apiKeyCache;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final MerchantContext merchantContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BASIC_PREFIX)) {
            log.debug("No API key credentials on {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String[] credentials = decode(header.substring(BASIC_PREFIX.length()));
        if (credentials == null) {
            log.warn("Malformed API key credentials for {} {}", request.getMethod(), request.getRequestURI());
            // filters run before the dispatcher, so hand the error to GlobalExceptionHandler through the resolver
            handlerExceptionResolver.resolveException(request, response, null,
                    new BadRequestException("MALFORMED_API_KEY_CREDENTIALS", "Authorization header must be Basic base64(keyId:keySecret)"));
            return;
        }

        // rate limit by keyId, before any DB/cache lookup, so a brute-force run against one key cannot
        // drown out the rest of the API — a wrong key never even reaches the real authentication check
        RatelimitResult rateLimitResult = rateLimiter.allow(
                ApiKeyRateLimitPolicy.KEY_PREFIX + credentials[0], ApiKeyRateLimitPolicy.MAX_REQUESTS, ApiKeyRateLimitPolicy.WINDOW);

        // set on every outcome, allowed or denied, so a cooperative client can see its quota and self-throttle
        // instead of waiting to be rejected
        response.setHeader("X-RateLimit-Limit", String.valueOf(ApiKeyRateLimitPolicy.MAX_REQUESTS));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitResult.remaining()));

        if (!rateLimitResult.allowed()) {
            log.warn("Rate limit exceeded for API key {} on {} {}", credentials[0], request.getMethod(), request.getRequestURI());
            handlerExceptionResolver.resolveException(request, response, null,
                    new RateLimitExceededException("RATE_LIMIT_EXCEEDED", "Too many requests for this API key", rateLimitResult.retryAfterSeconds()));
            return;
        }

        UUID merchantId;
        try {
            merchantId = authenticate(credentials[0], credentials[1]);
        } catch (InvalidCredentialsException ex) {
            log.warn("Rejected API key credentials for {} {}", request.getMethod(), request.getRequestURI());
            // filters run before the dispatcher, so hand the error to GlobalExceptionHandler through the resolver
            handlerExceptionResolver.resolveException(request, response, null, ex);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                credentials[0], null, List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        merchantContext.setMerchantId(merchantId);
        log.debug("Authenticated API key {} for {} {}", credentials[0], request.getMethod(), request.getRequestURI());

        filterChain.doFilter(request, response);
    }

    // same error for unknown key, revoked key and wrong secret so callers cannot probe which key ids exist
    private UUID authenticate(String keyId, String keySecret) {

        ApiKeyCacheEntry entry = loadAndCache(keyId)
                .filter(ApiKeyCacheEntry::enabled)
                .filter(e -> secretMatches(e, keySecret))
                .orElseThrow(() -> new InvalidCredentialsException("INVALID_API_KEY", "Invalid API key or secret"));

        return entry.merchantId();
    }

    // cache-aside: serves the cached snapshot when present, otherwise loads the row once and caches it for next time
    private Optional<ApiKeyCacheEntry> loadAndCache(String keyId) {
        Optional<ApiKeyCacheEntry> cached = apiKeyCache.get(keyId);
        if (cached.isPresent()) {
            return cached;
        }

        return apiKeyRepository.findByKeyId(keyId)
                .map(this::toCacheEntry)
                .map(entry -> {
                    apiKeyCache.put(keyId, entry);
                    return entry;
                });
    }

    private ApiKeyCacheEntry toCacheEntry(ApiKey apiKey) {
        return new ApiKeyCacheEntry(
                apiKey.getMerchant().getId(),
                apiKey.getKeyId(),
                apiKey.getKeySecretHash(),
                apiKey.getPreviousKeySecretHash(),
                apiKey.getEnvironment(),
                apiKey.isEnabled(),
                apiKey.getGracePeriodExpiresAt()
        );
    }

    private boolean secretMatches(ApiKeyCacheEntry entry, String keySecret) {
        if (passwordEncoder.matches(keySecret, entry.keySecretHash())) {
            return true;
        }
        // after a rotation the previous secret keeps working until the grace period ends
        return entry.previousKeySecretHash() != null
                && entry.isInGracePeriod()
                && passwordEncoder.matches(keySecret, entry.previousKeySecretHash());
    }

    // returns {keyId, keySecret}, or null when the value is not valid Base64 or has no keyId:keySecret shape
    private String[] decode(String encoded) {
        try {
            String decoded = new String(Base64.getDecoder().decode(encoded.trim()), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator <= 0 || separator == decoded.length() - 1) {
                return null;
            }
            return new String[]{decoded.substring(0, separator), decoded.substring(separator + 1)};
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

}
