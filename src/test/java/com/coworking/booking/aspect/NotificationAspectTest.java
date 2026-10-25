package com.coworking.booking.aspect;

import com.coworking.booking.annotation.NotifyOnSuccess;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationAspectTest {

    @InjectMocks
    private NotificationAspect notificationAspect;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private NotifyOnSuccess notifyOnSuccess;

    @Test
    void testSendNotification_logsCorrectMessage() {
        // GIVEN: mock-об'єкти
        Object mockResult = "MockBookingDtoResult";
        when(notifyOnSuccess.action()).thenReturn("Тестова дія створення");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("create");

        // WHEN: викликаємо advice напряму (як це робить Spring під капотом)
        notificationAspect.sendNotification(joinPoint, notifyOnSuccess, mockResult);

        // THEN: перевіряємо, чи аспект успішно зчитав значення з нашої анотації
        verify(notifyOnSuccess, times(1)).action();
        verify(joinPoint, times(1)).getSignature();
    }
}