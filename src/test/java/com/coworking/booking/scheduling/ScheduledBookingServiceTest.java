package com.coworking.booking.scheduling;

import com.coworking.booking.entity.Booking;
import com.coworking.booking.entity.BookingStatus;
import com.coworking.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledBookingServiceTest {

    private static final Instant NOW_INSTANT = Instant.parse("2026-10-22T08:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(NOW_INSTANT, ZoneOffset.UTC);

    @Mock
    private BookingRepository bookingRepository;

    private ScheduledBookingService scheduledBookingService;

    @BeforeEach
    void setUp() {
        scheduledBookingService = new ScheduledBookingService(
                bookingRepository,
                Clock.fixed(NOW_INSTANT, ZoneOffset.UTC)
        );
    }

    @Test
    void runningReminderTaskTwiceQueuesEachReminderOnlyOnce() {
        Booking booking = new Booking();
        booking.setId(12L);
        booking.setCustomerEmail("guest@example.com");
        AtomicBoolean reminderClaimed = new AtomicBoolean();
        when(bookingRepository.findByStatusAndReminderSentAtIsNullAndStartsAtBetweenOrderByStartsAtAsc(
                eq(BookingStatus.CONFIRMED), eq(NOW), eq(NOW.plusHours(24)), any(Pageable.class)
        )).thenAnswer(invocation -> reminderClaimed.get() ? List.of() : List.of(booking));
        when(bookingRepository.markReminderSent(12L, NOW)).thenAnswer(invocation ->
                reminderClaimed.compareAndSet(false, true) ? 1 : 0
        );

        assertEquals(1, scheduledBookingService.sendUpcomingReminders(24, 100));
        assertEquals(0, scheduledBookingService.sendUpcomingReminders(24, 100));

        verify(bookingRepository).markReminderSent(12L, NOW);
    }

    @Test
    void expiredBookingTransitionIsIdempotent() {
        LocalDateTime cutoff = NOW.minusHours(24);
        when(bookingRepository.completeConfirmedBookingsBefore(
                cutoff, BookingStatus.CONFIRMED, BookingStatus.COMPLETED
        )).thenReturn(3, 0);

        assertEquals(3, scheduledBookingService.completeExpiredBookings(24));
        assertEquals(0, scheduledBookingService.completeExpiredBookings(24));

        verify(bookingRepository, org.mockito.Mockito.times(2)).completeConfirmedBookingsBefore(
                cutoff, BookingStatus.CONFIRMED, BookingStatus.COMPLETED
        );
    }

    @Test
    void invalidJobParametersAreRejectedByTheService() {
        assertThrows(IllegalArgumentException.class,
                () -> scheduledBookingService.sendUpcomingReminders(0, 100));
        assertThrows(IllegalArgumentException.class,
                () -> scheduledBookingService.sendUpcomingReminders(24, 0));
        assertThrows(IllegalArgumentException.class,
                () -> scheduledBookingService.completeExpiredBookings(-1));
    }
}
