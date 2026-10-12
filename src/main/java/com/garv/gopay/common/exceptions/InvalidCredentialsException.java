package com.garv.gopay.common.exceptions;

import lombok.Getter;

@Getter
public class InvalidCredentialsException extends RuntimeException {
    String errorCode;
    public InvalidCredentialsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
