package com.coworking.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Acceptance criteria 6-8 (GIVEN / Setup): Реєстрація кастомного резолвера в Spring MVC.
    // Щоб Spring взагалі дізнався про існування нашого TenantIdArgumentResolver і почав
    // використовувати його для параметрів з анотацією @TenantId, ми маємо явно додати його
    // до списку активних резолверів фреймворку через інтерфейс WebMvcConfigurer.
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new TenantIdArgumentResolver());
    }
}