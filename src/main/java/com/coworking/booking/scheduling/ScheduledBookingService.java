package com.coworking.booking.scheduling;

import com.coworking.booking.entity.Booking;
import com.coworking.booking.entity.BookingStatus;
import com.coworking.booking.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ScheduledBookingService {

    private final BookingRepository bookingRepository;
    private final Clock clock;

    public ScheduledBookingService(BookingRepository bookingRepository, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @Transactional
    public int sendUpcomingReminders(int lookAheadHours, int batchSize) {
        if (lookAheadHours <= 0 || batchSize <= 0) {
            throw new IllegalArgumentException("Reminder window and batch size must be greater than zero");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<Booking> candidates = bookingRepository
                .findByStatusAndReminderSentAtIsNullAndStartsAtBetweenOrderByStartsAtAsc(
                        BookingStatus.CONFIRMED,
                        now,
                        now.plusHours(lookAheadHours),
                        PageRequest.of(0, batchSize)
                );

        int remindersQueued = 0;
        for (Booking booking : candidates) {
            if (bookingRepository.markReminderSent(booking.getId(), now) == 1) {
                remindersQueued++;
                log.info("Queued reminder for booking {} to {}", booking.getId(), booking.getCustomerEmail());
            }
        }
        return remindersQueued;
    }

    @Transactional
    public int completeExpiredBookings(int graceHours) {
        if (graceHours < 0) {
            throw new IllegalArgumentException("Grace period must not be negative");
        }

        LocalDateTime cutoff = LocalDateTime.now(clock).minusHours(graceHours);
        return bookingRepository.completeConfirmedBookingsBefore(
                cutoff,
                BookingStatus.CONFIRMED,
                BookingStatus.COMPLETED
        );
    }
}
