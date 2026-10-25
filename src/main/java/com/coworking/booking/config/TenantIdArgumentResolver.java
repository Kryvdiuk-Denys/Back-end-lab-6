package com.coworking.booking.config;

import com.coworking.booking.annotation.TenantId;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

// Тип 3 — Кастомний резолвер аргументів MVC

public class TenantIdArgumentResolver implements HandlerMethodArgumentResolver {

    // Spring автоматично викликає цей метод, щоб дізнатися, чи має він застосовувати цей резолвер
    // до конкретного параметра в методі контролера.
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // Acceptance criteria 6 (WHEN): Перевіряємо, чи параметр методу помічений нашою анотацією @TenantId
        // і чи має він тип String.
        return parameter.hasParameterAnnotation(TenantId.class) &&
                parameter.getParameterType().equals(String.class);
    }

    // Якщо supportsParameter повернув true, викликається цей метод для фактичного отримання значення.
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, // Доступ до сирого HTTP-запиту
                                  WebDataBinderFactory binderFactory) throws Exception {

        // Acceptance criteria 7 (WHEN / THEN): Динамічне обчислення значення з HTTP-запиту.
        // Ми втручаємося в процес обробки запиту і самостійно витягуємо потрібні дані (заголовок "X-Tenant-ID").
        String tenantId = webRequest.getHeader("X-Tenant-ID");

        // Отримуємо саму анотацію, щоб перевірити її налаштування
        TenantId annotation = parameter.getParameterAnnotation(TenantId.class);

        // Обробка параметра required(), який ми задали в нашій кастомній анотації @TenantId
        if (annotation != null && annotation.required() && (tenantId == null || tenantId.isEmpty())) {
            throw new IllegalArgumentException("Заголовок X-Tenant-ID є обов'язковим");
        }

        // Те, що повертає цей метод, автоматично підставиться як аргумент у метод контролера
        return tenantId != null ? tenantId : "default-tenant";
    }
}