package com.coworking.booking.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.annotation.*;

// Тип 2 — Композитна анотація (Composed Annotation)

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
// Acceptance criteria 4 (GIVEN / WHEN): Створення власної мета-анотації.
// Вона поєднує в собі одразу дві стандартні анотації Spring:
// 1. Маршрутизацію HTTP POST-запиту (через RequestMapping).
// 2. Встановлення HTTP-статусу 201 Created (замість стандартного 200 OK).
@RequestMapping(method = RequestMethod.POST)
@ResponseStatus(HttpStatus.CREATED)
public @interface CreateEndpoint {

    // Acceptance criteria 4 / 5: Використання @AliasFor.
    // Це дозволяє нам передавати шлях (наприклад, @CreateEndpoint(path = "/add"))
    // безпосередньо в нашу анотацію, а Spring під капотом "прокине" це значення у RequestMapping.
    @AliasFor(annotation = RequestMapping.class, attribute = "path")
    String[] path() default {};
}