package com.coworking.booking.config;

import com.coworking.booking.scheduling.AutowiringQuartzJobFactory;
import com.coworking.booking.scheduling.job.BookingReminderJob;
import com.coworking.booking.scheduling.job.ExpiredBookingCleanupJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class QuartzScheduleConfiguration {

    @Bean
    public JobDetail bookingReminderJobDetail() {
        return JobBuilder.newJob(BookingReminderJob.class)
                .withIdentity("booking-reminder", "booking-jobs")
                .withDescription("Queue reminders for confirmed bookings that are about to start")
                .usingJobData("lookAheadHours", 24)
                .usingJobData("batchSize", 100)
                .requestRecovery(true)
                .storeDurably()
                .build();
    }

    @Bean
    public JobDetail expiredBookingCleanupJobDetail() {
        return JobBuilder.newJob(ExpiredBookingCleanupJob.class)
                .withIdentity("expired-booking-cleanup", "maintenance-jobs")
                .withDescription("Mark ended confirmed bookings as completed")
                .usingJobData("graceHours", 24)
                .requestRecovery(true)
                .storeDurably()
                .build();
    }

    @Bean
    public CronTrigger bookingReminderFrequentTrigger(
            @Qualifier("bookingReminderJobDetail") JobDetail jobDetail,
            @Qualifier("bookingSchedulerTimeZone") TimeZone timeZone
    ) {
        return CronTriggerBuilderSupport.cronTrigger(
                jobDetail,
                "booking-reminder-every-15-minutes",
                "booking-triggers",
                "0 0/15 * * * ?",
                timeZone
        ).usingJobData("lookAheadHours", 6).build();
    }

    @Bean
    public CronTrigger bookingReminderDailyTrigger(
            @Qualifier("bookingReminderJobDetail") JobDetail jobDetail,
            @Qualifier("bookingSchedulerTimeZone") TimeZone timeZone
    ) {
        return CronTriggerBuilderSupport.cronTrigger(
                jobDetail,
                "booking-reminder-daily",
                "booking-triggers",
                "0 0 9 * * ?",
                timeZone
        ).usingJobData("batchSize", 250).build();
    }

    @Bean
    public CronTrigger expiredBookingCleanupTrigger(
            @Qualifier("expiredBookingCleanupJobDetail") JobDetail jobDetail,
            @Qualifier("bookingSchedulerTimeZone") TimeZone timeZone
    ) {
        return CronTriggerBuilderSupport.cronTrigger(
                jobDetail,
                "expired-booking-cleanup-daily",
                "maintenance-triggers",
                "0 30 2 * * ?",
                timeZone
        ).usingJobData("graceHours", 48).build();
    }

    @Bean(name = "bookingSchedulerTimeZone")
    public TimeZone bookingSchedulerTimeZone(
            @Value("${booking.quartz.time-zone:Europe/Kyiv}") String timeZoneId
    ) {
        return resolveTimeZone(timeZoneId);
    }

    @Bean
    public AutowiringQuartzJobFactory quartzJobFactory() {
        return new AutowiringQuartzJobFactory();
    }

    @Bean
    public SchedulerFactoryBeanCustomizer quartzJobFactoryCustomizer(AutowiringQuartzJobFactory jobFactory) {
        return schedulerFactoryBean -> schedulerFactoryBean.setJobFactory(jobFactory);
    }

    @Bean
    public java.time.Clock bookingClock() {
        return java.time.Clock.systemDefaultZone();
    }

    public static TimeZone resolveTimeZone(String timeZoneId) {
        try {
            return TimeZone.getTimeZone(ZoneId.of(timeZoneId));
        } catch (DateTimeException | NullPointerException exception) {
            return TimeZone.getTimeZone("GMT");
        }
    }

    private static class CronTriggerBuilderSupport {

        private static org.quartz.TriggerBuilder<CronTrigger> cronTrigger(
                JobDetail jobDetail,
                String name,
                String group,
                String cronExpression,
                TimeZone timeZone
        ) {
            CronScheduleBuilder schedule = CronScheduleBuilder.cronSchedule(cronExpression)
                    .inTimeZone(timeZone)
                    .withMisfireHandlingInstructionDoNothing();

            return org.quartz.TriggerBuilder.newTrigger()
                    .withIdentity(name, group)
                    .forJob(jobDetail)
                    .withSchedule(schedule);
        }
    }
}
