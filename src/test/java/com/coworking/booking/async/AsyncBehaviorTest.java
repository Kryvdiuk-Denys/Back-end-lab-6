package com.coworking.booking.async;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.coworking.booking.WorkspaceService;
import com.coworking.booking.config.AsyncConfiguration;
import com.coworking.booking.controller.ApiExceptionHandler;
import com.coworking.booking.dto.BookingDto;
import com.coworking.booking.dto.BookingPreparationRequest;
import com.coworking.booking.dto.BookingPreparationResult;
import com.coworking.booking.entity.Workspace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = {AsyncConfiguration.class, AsyncBehaviorTest.TestBeans.class})
@TestPropertySource(properties = {
        "booking.async.core-pool-size=2",
        "booking.async.max-pool-size=2",
        "booking.async.queue-capacity=8",
        "booking.async.thread-name-prefix=booking-async-test-",
        "booking.async.lookup-delay-ms=100",
        "booking.async.side-effect-delay-ms=150",
        "booking.async.preparation-timeout-ms=2000"
})
class AsyncBehaviorTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final LocalDateTime END = START.plusHours(2);

    @org.springframework.beans.factory.annotation.Autowired
    private AsyncProbeService asyncProbeService;

    @org.springframework.beans.factory.annotation.Autowired
    private AsyncRejectingProbe asyncRejectingProbe;

    @org.springframework.beans.factory.annotation.Autowired
    private BookingPreparationService bookingPreparationService;

    @org.springframework.beans.factory.annotation.Autowired
    private BookingAsyncLookupService bookingAsyncLookupService;

    @org.springframework.beans.factory.annotation.Autowired
    private BookingSideEffectService bookingSideEffectService;

    @org.springframework.beans.factory.annotation.Autowired
    private WorkspaceService workspaceService;

    private CountDownLatch releaseBlockedTasks;

    @AfterEach
    void releaseAnyBlockedTasks() {
        if (releaseBlockedTasks != null) {
            releaseBlockedTasks.countDown();
        }
    }

    @Test
    void asyncMethodReturnsBeforeSlowWorkAndRunsOnNamedPoolThread() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        releaseBlockedTasks = new CountDownLatch(1);

        long startNanos = System.nanoTime();
        CompletableFuture<String> result = asyncProbeService.blockUntilReleased(releaseBlockedTasks, started);
        long callMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertTrue(callMillis < 500, "the proxy call should return without waiting for the task");
        assertFalse(result.isDone());
        assertTrue(started.await(1, TimeUnit.SECONDS));
        releaseBlockedTasks.countDown();
        assertTrue(result.get(1, TimeUnit.SECONDS).startsWith("booking-async-test-"));
    }

    @Test
    void independentCallsRunAtTheSameTimeAndCombineTheirResults() throws Exception {
        CountDownLatch bothStarted = new CountDownLatch(2);

        CompletableFuture<String> first = asyncProbeService.meetAtBarrier(bothStarted);
        CompletableFuture<String> second = asyncProbeService.meetAtBarrier(bothStarted);
        String combined = first.thenCombine(second, (one, two) -> one + " + " + two)
                .get(2, TimeUnit.SECONDS);

        String[] threads = combined.split(" \\+ ");
        assertEquals(2, threads.length);
        assertNotEquals(threads[0], threads[1]);
    }

    @Test
    void preparationCombinesAvailabilityAndPriceFromParallelCalls() throws Exception {
        Workspace workspace = workspace();
        when(workspaceService.getEntity(42L)).thenReturn(workspace);

        BookingPreparationResult result = bookingPreparationService.prepare(
                new BookingPreparationRequest(42L, START, END)
        );

        assertEquals("READY", result.status());
        assertEquals(Boolean.TRUE, result.workspaceAvailable());
        assertEquals(new BigDecimal("50.00"), result.estimatedPrice());
    }

    @Test
    void futureExceptionsCanBeHandledAndVoidFailuresAreLogged() throws Exception {
        assertEquals("fallback", asyncProbeService.fail().exceptionally(exception -> "fallback")
                .get(1, TimeUnit.SECONDS));

        Logger logger = (Logger) LoggerFactory.getLogger(AsyncConfiguration.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            CountDownLatch started = new CountDownLatch(1);
            asyncProbeService.failVoid(started);
            assertTrue(started.await(1, TimeUnit.SECONDS));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (System.nanoTime() < deadline && appender.list.isEmpty()) {
                Thread.sleep(10);
            }

            assertTrue(appender.list.stream().anyMatch(event ->
                    event.getFormattedMessage().contains("Uncaught failure in @Async void method")));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void waitingOnFutureHasATimeLimit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        releaseBlockedTasks = new CountDownLatch(1);
        CompletableFuture<String> future = asyncProbeService.blockUntilReleased(releaseBlockedTasks, started);
        assertTrue(started.await(1, TimeUnit.SECONDS));

        assertThrows(TimeoutException.class, () -> future.get(30, TimeUnit.MILLISECONDS));
        releaseBlockedTasks.countDown();
        assertTrue(future.get(1, TimeUnit.SECONDS).startsWith("booking-async-test-"));
    }

    @Test
    void preparationReturnsServiceUnavailableWhenItsWaitLimitExpires() {
        Workspace workspace = workspace();
        when(workspaceService.getEntity(42L)).thenReturn(workspace);
        BookingPreparationService shortTimeoutService = new BookingPreparationService(
                workspaceService,
                bookingAsyncLookupService,
                1
        );

        assertThrows(AsyncProcessingUnavailableException.class, () -> shortTimeoutService.prepare(
                new BookingPreparationRequest(42L, START, END)
        ));
    }

    @Test
    void fullExecutorQueueRaisesTaskRejectedException() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        releaseBlockedTasks = new CountDownLatch(1);

        CompletableFuture<Void> running = asyncRejectingProbe.block(releaseBlockedTasks, started);
        assertTrue(started.await(1, TimeUnit.SECONDS));
        CompletableFuture<Void> queued = asyncRejectingProbe.block(releaseBlockedTasks, new CountDownLatch(1));

        assertThrows(TaskRejectedException.class,
                () -> asyncRejectingProbe.block(releaseBlockedTasks, new CountDownLatch(1)));

        assertEquals(503, new ApiExceptionHandler()
                .asyncUnavailable(new AsyncProcessingUnavailableException("Background queue is full"))
                .getStatusCode().value());

        releaseBlockedTasks.countDown();
        running.get(1, TimeUnit.SECONDS);
        queued.get(1, TimeUnit.SECONDS);
    }

    @Test
    void bookingCreationSideEffectsAreFireAndForget() {
        BookingDto booking = new BookingDto();
        booking.setId(1L);
        booking.setCustomerName("Test User");

        long startNanos = System.nanoTime();
        bookingSideEffectService.processBookingCreated(booking);
        long callMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertTrue(callMillis < 500, "booking creation must not wait for email, PDF, or access sync");
    }

    private Workspace workspace() {
        Workspace workspace = new Workspace();
        workspace.setId(42L);
        workspace.setHourlyRate(new BigDecimal("25.00"));
        return workspace;
    }

    @Configuration
    @Import({
            AsyncProbeService.class,
            AsyncRejectingProbe.class,
            BookingAsyncLookupService.class,
            BookingPreparationService.class,
            BookingSideEffectService.class
    })
    static class TestBeans {

        @Bean
        WorkspaceService workspaceService() {
            return mock(WorkspaceService.class);
        }

        @Bean(name = "rejectingExecutor")
        ThreadPoolTaskExecutor rejectingExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(1);
            executor.setThreadNamePrefix("booking-reject-test-");
            executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
            return executor;
        }
    }
}
