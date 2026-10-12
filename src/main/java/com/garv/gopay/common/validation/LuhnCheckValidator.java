package com.garv.gopay.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LuhnCheckValidator implements ConstraintValidator<LuhnCheck, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // let @NotBlank handle presence
        }

        int sum = 0;
        boolean alternate = false;

        for (int i = value.length() - 1; i >= 0; i--) {
            int digit = Character.digit(value.charAt(i), 10);
            if (digit < 0) {
                return false;
            }
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }

}
