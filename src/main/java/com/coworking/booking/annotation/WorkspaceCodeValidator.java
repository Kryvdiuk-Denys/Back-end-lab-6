package com.coworking.booking.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class WorkspaceCodeValidator implements ConstraintValidator<ValidWorkspaceCode, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Перевірку на NotNull залишаємо стандартній анотації @NotBlank, якщо вона потрібна
        }
        return value.matches("^WS-\\d+$");
    }
}