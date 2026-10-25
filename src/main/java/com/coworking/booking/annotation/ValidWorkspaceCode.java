package com.coworking.booking.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = WorkspaceCodeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidWorkspaceCode {
    String message() default "Код робочого простору повинен починатися з 'WS-' та містити лише цифри після префікса";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}