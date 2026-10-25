package com.coworking.booking.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogFailure {
    // Додаємо категорію помилки (наприклад: "DATABASE", "BUSINESS_LOGIC", "SECURITY")
    String category() default "BUSINESS_LOGIC";
}