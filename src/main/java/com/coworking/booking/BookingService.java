package com.coworking.booking;

import com.coworking.booking.annotation.LogFailure;
import com.coworking.booking.annotation.NotifyOnSuccess;
import com.coworking.booking.annotation.TrackExecutionTime;
import com.coworking.booking.async.AsyncProcessingUnavailableException;
import com.coworking.booking.async.BookingSideEffectService;
import com.coworking.booking.dto.BookingDto;
import com.coworking.booking.entity.Booking;
import com.coworking.booking.entity.BookingStatus;
import com.coworking.booking.entity.Workspace;
import com.coworking.booking.mapper.BookingMapper;
import com.coworking.booking.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final WorkspaceService workspaceService;
    private final BookingMapper bookingMapper;
    private final BookingSideEffectService bookingSideEffectService;

    public BookingService(
            BookingRepository bookingRepository,
            WorkspaceService workspaceService,
            BookingMapper bookingMapper,
            BookingSideEffectService bookingSideEffectService
    ) {
        this.bookingRepository = bookingRepository;
        this.workspaceService = workspaceService;
        this.bookingMapper = bookingMapper;
        this.bookingSideEffectService = bookingSideEffectService;
    }

    @Transactional(readOnly = true)
    public List<BookingDto> findAll() {
        return bookingMapper.toDtoList(bookingRepository.findAllByOrderByStartsAtDesc());
    }

    @TrackExecutionTime
    @Transactional(readOnly = true)
    public BookingDto findById(Long id) {
        log.info("Виклик реального методу findById для id: {}", id);
        return bookingMapper.toDto(getEntity(id));
    }

    public BookingDto findByIdWithSelfInvocationDemo(Long id) {
        log.info("Виклик findById через self-invocation (аспект не повинен спрацювати)");
        return this.findById(id);
    }

    @NotifyOnSuccess(action = "Створення нового бронювання")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Transactional
    public BookingDto create(BookingDto dto) {
        validateRequest(dto);
        Booking booking = bookingMapper.toEntity(dto);
        applyBusinessFields(booking, dto);
        BookingDto createdBooking = bookingMapper.toDto(bookingRepository.save(booking));
        try {
            bookingSideEffectService.processBookingCreated(createdBooking);
        } catch (TaskRejectedException exception) {
            throw new AsyncProcessingUnavailableException(
                    "Background booking tasks are busy. Please try again later.", exception
            );
        }
        return createdBooking;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional
    public BookingDto update(Long id, BookingDto dto) {
        validateRequest(dto);
        Booking booking = getEntity(id);
        bookingMapper.updateEntityFromDto(dto, booking);
        applyBusinessFields(booking, dto);
        return bookingMapper.toDto(booking);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        bookingRepository.delete(getEntity(id));
    }

    private void applyBusinessFields(Booking booking, BookingDto dto) {
        validatePeriod(booking);
        Workspace workspace = workspaceService.getEntity(dto.getWorkspaceId());
        booking.setWorkspace(workspace);
        if (booking.getStatus() == null) {
            booking.setStatus(BookingStatus.CONFIRMED);
        }
        booking.setTotalPrice(calculateTotalPrice(workspace, booking));
    }

    private void validateRequest(BookingDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Booking data must not be null");
        }
        if (dto.getWorkspaceId() == null) {
            throw new IllegalArgumentException("Workspace ID must not be null");
        }
    }

    private void validatePeriod(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking must not be null");
        }
        if (booking.getStartsAt() == null || booking.getEndsAt() == null) {
            throw new IllegalArgumentException("Booking start and end times must not be null");
        }
        if (!booking.getEndsAt().isAfter(booking.getStartsAt())) {
            throw new IllegalArgumentException("Booking end time must be after start time");
        }
    }

    private BigDecimal calculateTotalPrice(Workspace workspace, Booking booking) {
        long minutes = Duration.between(booking.getStartsAt(), booking.getEndsAt()).toMinutes();
        return workspace.getHourlyRate()
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    @LogFailure(category = "DATABASE_LOOKUP")
    @Transactional(readOnly = true)
    public Booking getEntity(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + id + " not found"));
    }
}
