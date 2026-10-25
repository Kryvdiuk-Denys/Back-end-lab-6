package com.coworking.booking.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    private final ObjectProvider<ThreadPoolTaskExecutor> executorProvider;

    public AsyncConfiguration(
            @Qualifier("bookingTaskExecutor") ObjectProvider<ThreadPoolTaskExecutor> executorProvider
    ) {
        this.executorProvider = executorProvider;
    }

    @Bean(name = {"bookingTaskExecutor", "taskExecutor"})
    public ThreadPoolTaskExecutor bookingTaskExecutor(
            @Value("${booking.async.core-pool-size:4}") int corePoolSize,
            @Value("${booking.async.max-pool-size:4}") int maxPoolSize,
            @Value("${booking.async.queue-capacity:50}") int queueCapacity,
            @Value("${booking.async.thread-name-prefix:booking-async-}") String threadNamePrefix
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return executorProvider.getObject();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (exception, method, parameters) -> log.error(
                "Uncaught failure in @Async void method {} with parameters {}",
                method.toGenericString(),
                Arrays.toString(parameters),
                exception
        );
    }
}
