package com.coworking.booking.config;

import com.coworking.booking.annotation.TenantId;
// Якщо TenantIdArgumentResolver так і залишився в папці annotation в src/main,
// IDE сама запропонує його імпортувати. Якщо він у config - імпорт не потрібен.
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantIdArgumentResolverTest {

    private TenantIdArgumentResolver resolver;
    private NativeWebRequest webRequest;

    @BeforeEach
    void setUp() {
        resolver = new TenantIdArgumentResolver();
        webRequest = mock(NativeWebRequest.class);
    }

    // @SuppressWarnings каже IDE не підкреслювати невикористані параметри, бо так задумано
    @SuppressWarnings("unused")
    public void dummyMethod(@TenantId String tenantId, String notAnnotated) {}

    @Test
    void givenAnnotatedParameter_whenSupportsParameter_thenReturnsTrue() throws NoSuchMethodException {
        // GIVEN: параметри з анотацією і без неї
        Method method = getClass().getMethod("dummyMethod", String.class, String.class);
        MethodParameter annotatedParam = new MethodParameter(method, 0);
        MethodParameter unannotatedParam = new MethodParameter(method, 1);

        // WHEN / THEN: (Acceptance criteria 6)
        assertTrue(resolver.supportsParameter(annotatedParam), "Має повертати true для параметра з @TenantId");
        assertFalse(resolver.supportsParameter(unannotatedParam), "Має повертати false для параметра без анотації");
    }

    @Test
    void givenValidRequest_whenResolveArgument_thenReturnsExtractedValue() throws Exception {
        // GIVEN: Запит із заголовком X-Tenant-ID
        Method method = getClass().getMethod("dummyMethod", String.class, String.class);
        MethodParameter annotatedParam = new MethodParameter(method, 0);

        when(webRequest.getHeader("X-Tenant-ID")).thenReturn("customer-a-tenant");

        // WHEN: Викликається resolveArgument (Acceptance criteria 7)
        Object result = resolver.resolveArgument(annotatedParam, null, webRequest, null);

        // THEN: Повертається коректно видобуте значення
        assertEquals("customer-a-tenant", result);
    }
}