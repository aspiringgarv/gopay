package com.garv.gopay.vault.config;

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
public class VaultEncryptionConfig {

    @Value("${vault.master-key}")
    private String masterKeyBase64;

    @Bean
    public BytesEncryptor vaultMasterEncryptor() {
        return panEncryptor(Base64.getDecoder().decode(masterKeyBase64));
    }

    @Bean
    public BytesKeyGenerator vaultDekGenerator() {
        return KeyGenerators.secureRandom(32);
    }

    public BytesEncryptor panEncryptor(byte[] dek) {
        SecretKeySpec secretKeySpec = new SecretKeySpec(dek, "AES");
        return new AesBytesEncryptor(secretKeySpec, KeyGenerators.secureRandom(12), AesBytesEncryptor.CipherAlgorithm.GCM);
    }

}
