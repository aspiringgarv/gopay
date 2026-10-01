package com.garv.gopay.common.exceptions;

import lombok.Getter;

@Getter
public class DuplicateResourceException extends RuntimeException {
    String errorCode;
    public DuplicateResourceException(String errorCode ,String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
