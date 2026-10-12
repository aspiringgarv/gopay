package com.garv.gopay.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotPastYearValidator.class)
public @interface NotPastYear {

    String message() default "Year must not be in the past";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
