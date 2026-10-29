package com.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class FutureDateTimeValidator implements ConstraintValidator<FutureDateTime, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        // null/blank is handled by @NotBlank — let it pass here to avoid duplicate messages
        if (value == null || value.isBlank()) return true;

        try {
            LocalDateTime dt = LocalDateTime.parse(value);
            if (dt.isAfter(LocalDateTime.now())) return true;

            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("Appointment date must be in the future")
               .addConstraintViolation();
            return false;

        } catch (DateTimeParseException e) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(
                    "Invalid date format — use ISO 8601: 2026-11-15T09:30:00")
               .addConstraintViolation();
            return false;
        }
    }
}
