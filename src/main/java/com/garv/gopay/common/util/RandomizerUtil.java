package com.garv.gopay.common.util;

import java.security.SecureRandom;
import java.util.Base64;

public class RandomizerUtil {
    private static final SecureRandom secureRandom = new SecureRandom();
    public static String randomBase64(int length){
        byte buf[] = new byte[length/2];
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf); // will genrate  (len*(3/4))
    }
}
