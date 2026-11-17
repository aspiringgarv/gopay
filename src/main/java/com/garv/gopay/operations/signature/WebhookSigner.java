package com.garv.gopay.operations.signature;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class WebhookSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private WebhookSigner() {
    }

    public static String sign(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign webhook payload", ex);
        }
    }

}
