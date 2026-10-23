package com.garv.gopay.common.ratelimiting.cache;

import java.time.Duration;

public final class ApiKeyRateLimitPolicy {

    public static final String KEY_PREFIX = "apikey:";
    public static final int MAX_REQUESTS = 100;
    public static final Duration WINDOW = Duration.ofMinutes(1);

    private ApiKeyRateLimitPolicy() {
    }

}
