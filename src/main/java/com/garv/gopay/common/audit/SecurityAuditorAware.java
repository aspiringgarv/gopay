package com.garv.gopay.common.audit;

import com.garv.gopay.common.enums.Actor;
import com.garv.gopay.merchant.entities.AppUser;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

// feeds @CreatedBy / @LastModifiedBy on BaseEntity from whoever the request's filter authenticated:
// an AppUser (email) on the JWT chain, an API key id on the API key chain, or SYSTEM with no caller at all
@Component
public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(Actor.SYSTEM.name());
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUser appUser) {
            return Optional.of(appUser.getUsername());
        }

        return Optional.of(String.valueOf(principal));
    }

}
