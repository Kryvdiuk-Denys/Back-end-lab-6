package com.coworking.booking.scheduling.job;

import com.coworking.booking.scheduling.QuartzJobParameters;
import com.coworking.booking.scheduling.ScheduledBookingService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@DisallowConcurrentExecution
public class ExpiredBookingCleanupJob implements Job {

    @Autowired
    private ScheduledBookingService scheduledBookingService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        int graceHours = QuartzJobParameters.requireNonNegativeInt(context, "graceHours");

        try {
            int completedBookings = scheduledBookingService.completeExpiredBookings(graceHours);
            log.info("Quartz expired booking job completed {} bookings", completedBookings);
        } catch (RuntimeException exception) {
            log.error("Expired booking job failed; it will run again at its next scheduled time", exception);
        }
    }
}
