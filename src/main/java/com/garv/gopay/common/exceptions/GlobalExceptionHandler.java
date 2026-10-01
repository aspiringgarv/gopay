package com.garv.gopay.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException duplicateResourceException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(duplicateResourceException.getErrorCode(), duplicateResourceException.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException resourceNotFoundException) {
        String errorCode = resourceNotFoundException.getResourceName()+"_NOT_FOUND";
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(errorCode, resourceNotFoundException.getMessage()));
    }

    @ExceptionHandler(ApiKeyRevokedException.class)
    public ResponseEntity<ErrorResponse> handleApiKeyRevokedException(ApiKeyRevokedException apiKeyRevokedException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(apiKeyRevokedException.getErrorCode(), apiKeyRevokedException.getMessage()));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolationException(BusinessRuleViolationException businessRuleViolationException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(businessRuleViolationException.getErrorCode(), businessRuleViolationException.getMessage()));
    }
}
