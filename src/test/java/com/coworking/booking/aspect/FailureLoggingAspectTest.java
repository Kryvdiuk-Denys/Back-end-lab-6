package com.coworking.booking.aspect;

import com.coworking.booking.annotation.LogFailure;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailureLoggingAspectTest {

    @InjectMocks
    private FailureLoggingAspect failureLoggingAspect;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private LogFailure logFailure;

    @Test
    void testLogException_readsAnnotationAndLogsError() {
        // GIVEN: налаштовуємо mock-об'єкти
        Throwable mockException = new RuntimeException("Тестова помилка бази даних");
        when(logFailure.category()).thenReturn("DATABASE_LOOKUP");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("getEntity");

        // WHEN: імітуємо перехоплення винятку аспектом
        failureLoggingAspect.logException(joinPoint, logFailure, mockException);

        // THEN: перевіряємо, чи аспект успішно зчитав параметри анотації та ім'я методу
        verify(logFailure, times(1)).category();
        verify(joinPoint, times(1)).getSignature();
    }
}