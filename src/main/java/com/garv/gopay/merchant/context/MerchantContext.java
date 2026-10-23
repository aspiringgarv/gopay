package com.garv.gopay.merchant.context;

import com.garv.gopay.common.exceptions.InvalidCredentialsException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

// one instance per HTTP request (held in the request thread via RequestContextHolder), filled by JwtAuthenticationFilter
@Component
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@Getter
@Setter
public class MerchantContext {

    private UUID merchantId;

    public UUID requireMerchantId() {
        if (merchantId == null) {
            throw new InvalidCredentialsException("UNAUTHENTICATED", "No authenticated merchant for this request");
        }
        return merchantId;
    }

}
