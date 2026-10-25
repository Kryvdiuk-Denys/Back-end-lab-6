package com.coworking.booking.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

    @Around("@annotation(com.coworking.booking.annotation.TrackExecutionTime)")
    public Object trackTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            // Продовжуємо виконання цільового методу
            return joinPoint.proceed();
        } finally {
            long endTime = System.currentTimeMillis();
            log.info("AOP ExecutionTimeTracker: Метод {} виконано за {} мс",
                    joinPoint.getSignature().toShortString(), (endTime - startTime));
        }
    }
}