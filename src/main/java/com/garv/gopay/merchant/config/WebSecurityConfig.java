package com.garv.gopay.merchant.config;

import com.garv.gopay.common.enums.UserRole;
import com.garv.gopay.common.ratelimiting.RateLimiter;
import com.garv.gopay.common.ratelimiting.cache.ApiKeyCache;
import com.garv.gopay.merchant.context.MerchantContext;
import com.garv.gopay.merchant.repositories.ApiKeyRepository;
import com.garv.gopay.merchant.services.MerchantUserDetailsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class WebSecurityConfig {

    // dashboard-style routes, authenticated with a login JWT
    private static final String[] JWT_ROUTES = {"/v1/auth/**", "/v1/merchants/**", "/v1/admin/**", "/actuator/**"};

    // merchant API routes, authenticated with an API key id and secret
    private static final String[] API_KEY_ROUTES = {"/v1/orders/**", "/v1/payments/**", "/v1/vault/**"};

    @Bean
    @Order(1)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http,
                                                   JwtUtil jwtUtil,
                                                   MerchantUserDetailsService merchantUserDetailsService,
                                                   @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver,
                                                   MerchantContext merchantContext) throws Exception {
        http
                .securityMatcher(JWT_ROUTES)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/auth/signup", "/v1/auth/login", "/v1/auth/refresh", "/v1/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, merchantUserDetailsService, handlerExceptionResolver, merchantContext),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiKeySecurityFilterChain(HttpSecurity http,
                                                         ApiKeyRepository apiKeyRepository,
                                                         ApiKeyCache apiKeyCache,
                                                         PasswordEncoder passwordEncoder,
                                                         RateLimiter rateLimiter,
                                                         @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver,
                                                         MerchantContext merchantContext) throws Exception {
        http
                .securityMatcher(API_KEY_ROUTES)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(new ApiKeyAuthenticationFilter(apiKeyRepository, apiKeyCache, passwordEncoder, rateLimiter, handlerExceptionResolver, merchantContext),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(MerchantUserDetailsService merchantUserDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(merchantUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
