package com.garv.gopay.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class WebhookEncryptionConfig {

    @Value("${webhook.master-key}")
    private String masterKeyBase64;

    @Bean
    public BytesEncryptor webhookMasterEncryptor() {
        return secretEncryptor(Base64.getDecoder().decode(masterKeyBase64));
    }

    @Bean
    public BytesKeyGenerator webhookDekGenerator() {
        return KeyGenerators.secureRandom(32);
    }

    public BytesEncryptor secretEncryptor(byte[] dek) {
        SecretKeySpec secretKeySpec = new SecretKeySpec(dek, "AES");
        return new AesBytesEncryptor(secretKeySpec, KeyGenerators.secureRandom(12), AesBytesEncryptor.CipherAlgorithm.GCM);
    }

}
