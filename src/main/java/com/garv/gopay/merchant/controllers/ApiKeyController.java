package com.garv.gopay.merchant.controllers;

import com.garv.gopay.merchant.context.MerchantContext;
import com.garv.gopay.merchant.dto.request.CreateApiKeyRequest;
import com.garv.gopay.merchant.dto.response.ApiKeyResponse;
import com.garv.gopay.merchant.dto.response.ApikeyCreateResponse;
import com.garv.gopay.merchant.services.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/merchants/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final MerchantContext merchantContext;

    @PostMapping()
    public ResponseEntity<ApikeyCreateResponse> create(@RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiKeyService.create(merchantContext.requireMerchantId(), request));
    }

    @GetMapping()
    public ResponseEntity<List<ApiKeyResponse>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(apiKeyService.getAll(merchantContext.requireMerchantId()));
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revoke(@PathVariable String keyId) {
        apiKeyService.revoke(merchantContext.requireMerchantId(), keyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{keyId}/rotate")
    public ResponseEntity<ApikeyCreateResponse> rotate(@PathVariable String keyId) {
        return ResponseEntity.ok(apiKeyService.rotate(merchantContext.requireMerchantId(), keyId));
    }

}
