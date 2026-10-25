package com.coworking.booking.repository;

import com.coworking.booking.entity.Booking;
import com.coworking.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = "workspace")
    List<Booking> findAllByOrderByStartsAtDesc();

    boolean existsByWorkspaceId(Long workspaceId);

    @EntityGraph(attributePaths = "workspace")
    List<Booking> findByStatusAndReminderSentAtIsNullAndStartsAtBetweenOrderByStartsAtAsc(
            BookingStatus status,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true)
    @Query("update Booking b set b.reminderSentAt = :sentAt " +
            "where b.id = :bookingId and b.reminderSentAt is null")
    int markReminderSent(@Param("bookingId") Long bookingId, @Param("sentAt") LocalDateTime sentAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Booking b set b.status = :completed " +
            "where b.status = :confirmed and b.endsAt < :cutoff")
    int completeConfirmedBookingsBefore(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("confirmed") BookingStatus confirmed,
            @Param("completed") BookingStatus completed
    );
}
