package com.hhm.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom constraint: the annotated String must be a valid ISO 8601 LocalDateTime
 * AND must represent a point in time strictly after now.
 *
 * Usage: @FutureDateTime on a String field that holds an appointment date.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureDateTimeValidator.class)
@Documented
public @interface FutureDateTime {
    String message() default "Appointment date must be in the future (format: 2026-11-15T09:30:00)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
