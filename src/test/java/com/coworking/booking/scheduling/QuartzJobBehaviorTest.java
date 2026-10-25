package com.coworking.booking.scheduling;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.coworking.booking.scheduling.job.BookingReminderJob;
import com.coworking.booking.scheduling.job.ExpiredBookingCleanupJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class QuartzJobBehaviorTest {

    private ScheduledBookingService scheduledBookingService;

    @BeforeEach
    void setUp() {
        scheduledBookingService = mock(ScheduledBookingService.class);
    }

    @Test
    void reminderJobReadsMergedParametersAndCallsOneTransactionalServiceMethod() throws Exception {
        BookingReminderJob job = new BookingReminderJob();
        setField(job, "scheduledBookingService", scheduledBookingService);
        JobExecutionContext context = context(Map.of("lookAheadHours", 6, "batchSize", 250));

        job.execute(context);

        verify(scheduledBookingService).sendUpcomingReminders(6, 250);
        assertTrueTransactional("sendUpcomingReminders", int.class, int.class);
    }

    @Test
    void missingOrIncorrectParameterFailsClearlyBeforeCallingService() {
        BookingReminderJob job = new BookingReminderJob();
        setField(job, "scheduledBookingService", scheduledBookingService);

        assertThrows(JobExecutionException.class, () -> job.execute(context(Map.of("batchSize", 10))));
        assertThrows(JobExecutionException.class, () -> job.execute(
                context(Map.of("lookAheadHours", "six", "batchSize", 10))
        ));
        verify(scheduledBookingService, never()).sendUpcomingReminders(0, 10);
    }

    @Test
    void cleanupJobUsesItsMergedGracePeriod() throws Exception {
        ExpiredBookingCleanupJob job = new ExpiredBookingCleanupJob();
        setField(job, "scheduledBookingService", scheduledBookingService);

        job.execute(context(Map.of("graceHours", 48)));

        verify(scheduledBookingService).completeExpiredBookings(48);
        assertTrue(ExpiredBookingCleanupJob.class.isAnnotationPresent(DisallowConcurrentExecution.class));
        assertTrueTransactional("completeExpiredBookings", int.class);
    }

    @Test
    void serviceFailureIsLoggedAndLeftForTheNextScheduledRun() throws Exception {
        BookingReminderJob job = new BookingReminderJob();
        setField(job, "scheduledBookingService", scheduledBookingService);
        when(scheduledBookingService.sendUpcomingReminders(6, 100))
                .thenThrow(new IllegalStateException("simulated database failure"));

        Logger logger = (Logger) LoggerFactory.getLogger(BookingReminderJob.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertDoesNotThrow(() -> job.execute(context(Map.of("lookAheadHours", 6, "batchSize", 100))));
            assertTrue(appender.list.stream().anyMatch(event ->
                    event.getFormattedMessage().contains("next scheduled time")));
            verify(scheduledBookingService).sendUpcomingReminders(6, 100);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private JobExecutionContext context(Map<String, Object> mergedParameters) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap(mergedParameters));
        return context;
    }

    private void assertTrueTransactional(String methodName, Class<?>... parameterTypes) throws Exception {
        assertTrue(ScheduledBookingService.class
                .getMethod(methodName, parameterTypes)
                .isAnnotationPresent(Transactional.class));
    }
}
