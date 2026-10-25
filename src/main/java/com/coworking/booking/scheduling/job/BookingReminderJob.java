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
public class BookingReminderJob implements Job {

    @Autowired
    private ScheduledBookingService scheduledBookingService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        int lookAheadHours = QuartzJobParameters.requirePositiveInt(context, "lookAheadHours");
        int batchSize = QuartzJobParameters.requirePositiveInt(context, "batchSize");

        try {
            int remindersQueued = scheduledBookingService.sendUpcomingReminders(lookAheadHours, batchSize);
            log.info("Quartz booking reminder job queued {} reminders", remindersQueued);
        } catch (RuntimeException exception) {
            log.error("Booking reminder job failed; it will run again at its next scheduled time", exception);
        }
    }
}
