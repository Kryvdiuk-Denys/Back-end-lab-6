package com.coworking.booking.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
// Acceptance criteria 1 (GIVEN): Зв'язуємо анотацію з класом валідатора за допомогою @Constraint(validatedBy = ...).
// Саме цей рядок забезпечує виконання умови THEN: Bean Validation автоматично знаходить і викликає цей клас
// без ручного створення (new Validator()).
@Constraint(validatedBy = BookingTimeValidator.class)
// Acceptance criteria 1 (GIVEN): Вказуємо ElementType.TYPE, що робить цю анотацію застосовною виключно на рівні
// всього класу (DTO), а не окремих полів.
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBookingTime {

    // Acceptance criteria 3 (THEN): Це дефолтне повідомлення, яке повернеться клієнту, якщо порушено логіку валідації.
    String message() default "Час початку бронювання має бути раніше за час закінчення";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}