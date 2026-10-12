package com.garv.gopay.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Year;

public class NotPastYearValidator implements ConstraintValidator<NotPastYear, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // let @NotNull handle presence
        }
        return value >= Year.now().getValue();
    }

}
