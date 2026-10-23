package com.garv.gopay.common.ratelimiting;

import java.time.Duration;

public interface RateLimiter {

    /** Checks whether the caller identified by {@code key} may proceed within the current window. */
    RatelimitResult allow(String key, int limit, Duration window);

}
