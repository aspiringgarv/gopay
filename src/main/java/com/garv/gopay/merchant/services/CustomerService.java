package com.garv.gopay.merchant.services;

import com.garv.gopay.merchant.entities.Customer;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public interface CustomerService {

    /**
     * Returns the merchant's customer with this email, creating one if none exists.
     * Email is matched trimmed and case-insensitively; an existing customer is returned as-is,
     * so name and phone are only applied when a new customer is created.
     * Returns null when the email is null or blank.
     */
    @Nullable Customer findOrCreate(UUID merchantId, String email, String name, String phone);

}
