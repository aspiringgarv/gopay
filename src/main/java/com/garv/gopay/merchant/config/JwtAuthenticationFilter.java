package com.garv.gopay.merchant.config;

import com.garv.gopay.common.exceptions.InvalidCredentialsException;
import com.garv.gopay.merchant.context.MerchantContext;
import com.garv.gopay.merchant.services.MerchantUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

// not a @Component on purpose: it is created in WebSecurityConfig so it only runs inside the security filter chain
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final MerchantUserDetailsService merchantUserDetailsService;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final MerchantContext merchantContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());
            if (jwtUtil.isValid(token)) {
                try {
                    authenticate(request, token);
                } catch (UsernameNotFoundException ex) {
                    // filters run before the dispatcher, so hand the error to GlobalExceptionHandler through the resolver
                    log.warn("JWT is valid but its user no longer exists, rejecting {} {}", request.getMethod(), request.getRequestURI());
                    handlerExceptionResolver.resolveException(request, response, null,
                            new InvalidCredentialsException("INVALID_TOKEN", "Token user no longer exists"));
                    return;
                }
            } else {
                log.warn("Rejected invalid or expired JWT for {} {}", request.getMethod(), request.getRequestURI());
            }
        } else {
            log.debug("No bearer token on {} {}", request.getMethod(), request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        UserDetails user = merchantUserDetailsService.loadUserByUsername(jwtUtil.extractEmail(token));
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        merchantContext.setMerchantId(jwtUtil.extractMerchantId(token));
        log.debug("Authenticated {} for {} {}", user.getUsername(), request.getMethod(), request.getRequestURI());
    }

}
