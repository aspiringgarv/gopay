package com.garv.gopay.vault.controllers;

import com.garv.gopay.merchant.context.MerchantContext;
import com.garv.gopay.vault.dto.request.TokenizeCardRequest;
import com.garv.gopay.vault.dto.response.TokenizeCardResponse;
import com.garv.gopay.vault.services.VaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/vault")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    private final MerchantContext merchantContext;

    @PostMapping("/tokenize")
    public ResponseEntity<TokenizeCardResponse> tokenize(@Valid @RequestBody TokenizeCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vaultService.tokenize(request, merchantContext.requireMerchantId()));
    }

}
