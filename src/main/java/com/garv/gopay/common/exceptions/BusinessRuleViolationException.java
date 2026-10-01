package com.garv.gopay.common.exceptions;

import lombok.Getter;

@Getter
public class BusinessRuleViolationException extends RuntimeException {
    String errorCode;
    public BusinessRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
