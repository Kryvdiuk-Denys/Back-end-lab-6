package com.coworking.booking.aspect;

import com.coworking.booking.annotation.LogFailure;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class FailureLoggingAspect {

    // Спрацьовує ТІЛЬКИ якщо метод викинув виняток (Throwable)
    @AfterThrowing(
            pointcut = "@annotation(logFailure)",
            throwing = "exception"
    )
    public void logException(JoinPoint joinPoint, LogFailure logFailure, Throwable exception) {
        String methodName = joinPoint.getSignature().getName();
        String category = logFailure.category();

        log.error("AOP FailureLogger: Помилка в категорії [{}] під час виконання методу '{}'. Деталі: {}",
                category, methodName, exception.getMessage());
    }
}