package com.coworking.booking.aspect;

import com.coworking.booking.annotation.NotifyOnSuccess;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class NotificationAspect {

    // Спрацьовує ТІЛЬКИ якщо метод успішно повернув результат
    @AfterReturning(
            pointcut = "@annotation(notifyAnnotation)",
            returning = "result"
    )
    public void sendNotification(JoinPoint joinPoint, NotifyOnSuccess notifyAnnotation, Object result) {
        String action = notifyAnnotation.action();
        String methodName = joinPoint.getSignature().getName();

        // В реальному проекті тут міг би бути виклик EmailService або Kafka-продюсера
        log.info("AOP Notification: Дію [{}] з методу '{}' виконано успішно. Згенеровано результат: {}",
                action, methodName, result);
    }
}