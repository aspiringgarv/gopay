package com.garv.gopay.common.idempotency;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStore {

    // shared with callers (e.g. IdempotencyFilter) that need to tell "still being worked on" apart from
    // "finished, here is the real stored result" without hardcoding the marker string themselves
    String IN_PROGRESS = "IN_PROGRESS";

    /** Claims {@code key} only if nothing is stored there yet, with a fixed "in progress" marker value.
     *  Returns true if this call was the one that claimed it (first request), false if something was
     *  already there (a replay). */
    boolean setIfAbsent(String key, Duration ttl);

    /** Unconditionally stores the real outcome under {@code key}, overwriting the "in progress" marker
     *  (or anything else there) once the work it was guarding actually finishes. */
    void store(String key, String value, Duration ttl);

    Optional<String> get(String key);

    void delete(String key);

}
