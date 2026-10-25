package com.coworking.booking.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionTimeAspectTest {

    @InjectMocks
    private ExecutionTimeAspect executionTimeAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Test
    void testTrackTime_ProceedsAndReturnsResult() throws Throwable {
        // GIVEN: mock-об'єкт без контексту Spring
        String expectedResult = "TestResult";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("BookingService.findById(..)");
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // WHEN: викликаємо логіку аспекту
        Object actualResult = executionTimeAspect.trackTime(joinPoint);

        // THEN: перевіряємо, чи викликався proceed() і чи повернувся правильний результат
        verify(joinPoint, times(1)).proceed();
        assertEquals(expectedResult, actualResult);
    }
}