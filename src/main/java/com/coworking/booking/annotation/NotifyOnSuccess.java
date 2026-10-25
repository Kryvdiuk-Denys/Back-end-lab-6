package com.coworking.booking.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotifyOnSuccess {
    // Дозволяємо передавати кастомне повідомлення в анотацію
    String action() default "Операцію успішно завершено";
}