package com.garv.gopay.common.exceptions;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {
    String errorCode;
    int retryAfterSeconds;

    public RateLimitExceededException(String errorCode, String message, int retryAfterSeconds) {
        super(message);
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
