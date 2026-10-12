package com.garv.gopay.common.exceptions;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {
    String errorCode;
    public BadRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
