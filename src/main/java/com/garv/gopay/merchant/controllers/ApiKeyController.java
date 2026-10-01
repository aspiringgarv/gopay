package com.garv.gopay.merchant.controllers;

import com.garv.gopay.merchant.dto.request.CreateApiKeyRequest;
import com.garv.gopay.merchant.dto.response.ApiKeyResponse;
import com.garv.gopay.merchant.dto.response.ApikeyCreateResponse;
import com.garv.gopay.merchant.services.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/merchants/{merchantId}/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private  final ApiKeyService apiKeyService;

    @PostMapping()
    public ResponseEntity<ApikeyCreateResponse> create(
            @PathVariable UUID merchantId,
            @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiKeyService.create(merchantId, request));
    }

    @GetMapping()
    public ResponseEntity<List<ApiKeyResponse>> getAll(@PathVariable UUID merchantId) {
        return ResponseEntity.status(HttpStatus.OK).body(apiKeyService.getAll(merchantId));
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID merchantId, @PathVariable String keyId) {
        apiKeyService.revoke(merchantId, keyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{keyId}/rotate")
    public ResponseEntity<ApikeyCreateResponse> rotate(@PathVariable UUID merchantId, @PathVariable String keyId) {
        return ResponseEntity.ok(apiKeyService.rotate(merchantId, keyId));
    }

}
