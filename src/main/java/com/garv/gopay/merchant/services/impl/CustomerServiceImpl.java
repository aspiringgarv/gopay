package com.garv.gopay.merchant.services.impl;

import com.garv.gopay.common.exceptions.ResourceNotFoundException;
import com.garv.gopay.merchant.entities.Customer;
import com.garv.gopay.merchant.entities.Merchant;
import com.garv.gopay.merchant.repositories.CustomerRepository;
import com.garv.gopay.merchant.repositories.MerchantRepository;
import com.garv.gopay.merchant.services.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;

    @Override
    @Transactional
    public @Nullable Customer findOrCreate(UUID merchantId, String email, String name, String phone) {
        if (email == null || email.isBlank()) {
            return null;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return customerRepository.findByMerchant_IdAndEmail(merchantId, normalizedEmail)
                .orElseGet(() -> create(merchantId, normalizedEmail, name, phone));
    }

    private Customer create(UUID merchantId, String email, String name, String phone) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        Customer customer = customerRepository.save(Customer.builder()
                .merchant(merchant)
                .email(email)
                .name(name)
                .phone(phone)
                .build());

        log.info("Created customer {} for merchant {}", customer.getId(), merchantId);
        return customer;
    }

}
