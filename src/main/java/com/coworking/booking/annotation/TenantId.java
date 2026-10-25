package com.coworking.booking.annotation;

import java.lang.annotation.*;

// Тип 3 — Кастомна анотація для резолвера аргументів MVC (HandlerMethodArgumentResolver)

// Acceptance criteria 6 (GIVEN): Створення власної анотації, яка буде оброблятися кастомним
// резолвером.
// @Target(ElementType.PARAMETER) чітко обмежує використання цієї анотації:
// її можна ставити ТІЛЬКИ перед аргументами методів (наприклад, у контролері).
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TenantId {

    // Додатковий параметр нашої анотації.
    // Наш кастомний TenantIdArgumentResolver зможе прочитати це значення через рефлексію
    // і вирішити, чи кидати помилку (IllegalArgumentException), якщо заголовок запиту відсутній.
    boolean required() default true;
}