package com.garv.gopay.merchant.config;

import com.garv.gopay.common.exceptions.BadRequestException;
import com.garv.gopay.common.exceptions.InvalidCredentialsException;
import com.garv.gopay.merchant.context.MerchantContext;
import com.garv.gopay.merchant.services.ApiKeyService;
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
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

// authenticates merchant API calls with "Authorization: Basic base64(keyId:keySecret)"
// not a @Component on purpose: it is created in WebSecurityConfig so it only runs inside the API key filter chain
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String BASIC_PREFIX = "Basic ";

    private final ApiKeyService apiKeyService;
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

        try {
            UUID merchantId = apiKeyService.authenticate(credentials[0], credentials[1]);

            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                    credentials[0], null, List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            merchantContext.setMerchantId(merchantId);
            log.debug("Authenticated API key {} for {} {}", credentials[0], request.getMethod(), request.getRequestURI());
        } catch (InvalidCredentialsException ex) {
            log.warn("Rejected API key credentials for {} {}", request.getMethod(), request.getRequestURI());
            // filters run before the dispatcher, so hand the error to GlobalExceptionHandler through the resolver
            handlerExceptionResolver.resolveException(request, response, null, ex);
            return;
        }

        filterChain.doFilter(request, response);
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
