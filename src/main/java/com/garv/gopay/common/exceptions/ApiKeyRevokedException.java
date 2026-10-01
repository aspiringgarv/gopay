package com.garv.gopay.common.exceptions;

import lombok.Getter;

@Getter
public class ApiKeyRevokedException extends RuntimeException {
    String errorCode;
    public ApiKeyRevokedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
