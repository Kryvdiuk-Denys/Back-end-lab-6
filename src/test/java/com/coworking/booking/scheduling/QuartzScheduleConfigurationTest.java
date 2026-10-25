package com.coworking.booking.scheduling;

import com.coworking.booking.config.QuartzScheduleConfiguration;
import com.coworking.booking.scheduling.job.BookingReminderJob;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(QuartzScheduleConfiguration.class)
@TestPropertySource(properties = "booking.quartz.time-zone=Europe/Kyiv")
class QuartzScheduleConfigurationTest {

    @Autowired
    @Qualifier("bookingReminderJobDetail")
    private JobDetail reminderJob;

    @Autowired
    @Qualifier("expiredBookingCleanupJobDetail")
    private JobDetail cleanupJob;

    @Autowired
    @Qualifier("bookingReminderFrequentTrigger")
    private CronTrigger frequentReminderTrigger;

    @Autowired
    @Qualifier("bookingReminderDailyTrigger")
    private CronTrigger dailyReminderTrigger;

    @Autowired
    @Qualifier("expiredBookingCleanupTrigger")
    private CronTrigger cleanupTrigger;

    @Autowired
    @Qualifier("bookingSchedulerTimeZone")
    private TimeZone schedulerTimeZone;

    @Test
    void jobsAndTriggersHaveExplicitNamesGroupsAndJobKeys() {
        assertEquals(new JobKey("booking-reminder", "booking-jobs"), reminderJob.getKey());
        assertEquals(new JobKey("expired-booking-cleanup", "maintenance-jobs"), cleanupJob.getKey());

        assertEquals(new TriggerKey("booking-reminder-every-15-minutes", "booking-triggers"),
                frequentReminderTrigger.getKey());
        assertEquals(new TriggerKey("booking-reminder-daily", "booking-triggers"),
                dailyReminderTrigger.getKey());
        assertEquals(new TriggerKey("expired-booking-cleanup-daily", "maintenance-triggers"),
                cleanupTrigger.getKey());

        assertEquals(reminderJob.getKey(), frequentReminderTrigger.getJobKey());
        assertEquals(reminderJob.getKey(), dailyReminderTrigger.getJobKey());
        assertEquals(cleanupJob.getKey(), cleanupTrigger.getJobKey());
        assertTrue(reminderJob.requestsRecovery());
        assertTrue(cleanupJob.requestsRecovery());
    }

    @Test
    void everyTriggerHasExplicitMisfirePolicyAndTimeZone() {
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, frequentReminderTrigger.getMisfireInstruction());
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, dailyReminderTrigger.getMisfireInstruction());
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, cleanupTrigger.getMisfireInstruction());

        assertEquals("Europe/Kyiv", schedulerTimeZone.getID());
        assertEquals("Europe/Kyiv", frequentReminderTrigger.getTimeZone().getID());
        assertEquals("Europe/Kyiv", dailyReminderTrigger.getTimeZone().getID());
        assertEquals("Europe/Kyiv", cleanupTrigger.getTimeZone().getID());
    }

    @Test
    void quartzMergedDataUsesTriggerValuesAndRetainsOtherJobDefaults() throws Exception {
        JobExecutionContextImpl frequentContext = contextFor(reminderJob, frequentReminderTrigger);
        JobExecutionContextImpl dailyContext = contextFor(reminderJob, dailyReminderTrigger);

        assertEquals(6, frequentContext.getMergedJobDataMap().getInt("lookAheadHours"));
        assertEquals(100, frequentContext.getMergedJobDataMap().getInt("batchSize"));
        assertEquals(24, dailyContext.getMergedJobDataMap().getInt("lookAheadHours"));
        assertEquals(250, dailyContext.getMergedJobDataMap().getInt("batchSize"));

        ScheduledBookingService service = mock(ScheduledBookingService.class);
        BookingReminderJob job = new BookingReminderJob();
        ReflectionTestUtils.setField(job, "scheduledBookingService", service);
        job.execute(frequentContext);
        verify(service).sendUpcomingReminders(6, 100);
    }

    @Test
    void unknownTimeZoneFallsBackToGmt() {
        assertEquals("GMT", QuartzScheduleConfiguration.resolveTimeZone("Mars/Olympus").getID());
    }

    private JobExecutionContextImpl contextFor(JobDetail jobDetail, CronTrigger trigger) {
        Date now = new Date();
        TriggerFiredBundle bundle = new TriggerFiredBundle(
                jobDetail,
                (OperableTrigger) trigger,
                null,
                false,
                now,
                now,
                null,
                null
        );
        return new JobExecutionContextImpl(null, bundle, null);
    }
}
