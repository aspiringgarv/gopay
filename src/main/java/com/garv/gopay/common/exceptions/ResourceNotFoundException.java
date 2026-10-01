package com.garv.gopay.common.exceptions;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    String resourceName;
    Object identifier;

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resourceName + " not found with identifier: " + identifier);
        this.resourceName = resourceName;
        this.identifier = identifier;
    }
}
