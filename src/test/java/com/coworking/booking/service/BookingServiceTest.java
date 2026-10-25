package com.coworking.booking.service;

import com.coworking.booking.BookingService;
import com.coworking.booking.ResourceNotFoundException;
import com.coworking.booking.WorkspaceService;
import com.coworking.booking.async.AsyncProcessingUnavailableException;
import com.coworking.booking.async.BookingSideEffectService;
import com.coworking.booking.dto.BookingDto;
import com.coworking.booking.entity.Booking;
import com.coworking.booking.entity.BookingStatus;
import com.coworking.booking.entity.Workspace;
import com.coworking.booking.mapper.BookingMapper;
import com.coworking.booking.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 21, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 9, 21, 12, 30);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private BookingSideEffectService bookingSideEffectService;

    @InjectMocks
    private BookingService bookingService;

    @Test
    @DisplayName("findAll повертає список скоригованих DTO")
    void findAllExistingBookingsReturnsMappedDtos() {
        // Arrange
        Booking booking = booking(1L, workspace(10L), START, END, BookingStatus.CONFIRMED);
        BookingDto dto = bookingDto(1L, 10L, START, END, BookingStatus.CONFIRMED);
        when(bookingRepository.findAllByOrderByStartsAtDesc()).thenReturn(List.of(booking));
        when(bookingMapper.toDtoList(List.of(booking))).thenReturn(List.of(dto));

        // Act
        List<BookingDto> result = bookingService.findAll();

        // Assert
        assertEquals(List.of(dto), result);
        verify(bookingRepository).findAllByOrderByStartsAtDesc();
        verify(bookingMapper).toDtoList(List.of(booking));
    }

    @Test
    @DisplayName("findById з існуючим ID повертає DTO")
    void findByIdExistingBookingReturnsMappedDto() {
        // Arrange
        Booking booking = booking(1L, workspace(10L), START, END, BookingStatus.CONFIRMED);
        BookingDto dto = bookingDto(1L, 10L, START, END, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDto(booking)).thenReturn(dto);

        // Act
        BookingDto result = bookingService.findById(1L);

        // Assert
        assertSame(dto, result);
        verify(bookingRepository).findById(1L);
        verify(bookingMapper).toDto(booking);
    }

    @Test
    @DisplayName("findById з відсутнім ID викидає ResourceNotFoundException")
    void findByIdMissingBookingThrowsResourceNotFoundException() {
        // Arrange
        when(bookingRepository.findById(404L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookingService.findById(404L)
        );

        // Assert
        assertEquals("Booking 404 not found", exception.getMessage());
        verify(bookingRepository).findById(404L);
    }

    @Test
    @DisplayName("create з валідними даними розраховує вартість та зберігає бронювання")
    void createValidBookingCalculatesTotalPriceAndSavesBooking() {
        // Arrange
        BookingDto request = bookingDto(null, 10L, START, END, BookingStatus.CONFIRMED);
        Booking mappedBooking = booking(null, null, START, END, BookingStatus.CONFIRMED);
        Workspace workspace = workspace(10L);
        BookingDto response = bookingDto(5L, 10L, START, END, BookingStatus.CONFIRMED);

        when(bookingMapper.toEntity(request)).thenReturn(mappedBooking);
        when(workspaceService.getEntity(10L)).thenReturn(workspace);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toDto(mappedBooking)).thenReturn(response);

        // Act
        BookingDto result = bookingService.create(request);

        // Assert
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        Booking savedBooking = captor.getValue();

        assertSame(response, result);
        assertSame(workspace, savedBooking.getWorkspace());
        assertEquals(BookingStatus.CONFIRMED, savedBooking.getStatus());
        assertEquals(new BigDecimal("62.50"), savedBooking.getTotalPrice());
        verify(workspaceService).getEntity(10L);
        verify(bookingSideEffectService).processBookingCreated(response);
        verify(bookingMapper).toDto(mappedBooking);
    }

    @Test
    @DisplayName("create з NULL статусом встановлює дефолтний статус CONFIRMED")
    void createBookingWithNullStatusDefaultsStatusToConfirmed() {
        // Arrange
        BookingDto request = bookingDto(null, 10L, START, END, null);
        Booking mappedBooking = booking(null, null, START, END, null);
        Workspace workspace = workspace(10L);
        BookingDto response = bookingDto(5L, 10L, START, END, BookingStatus.CONFIRMED);

        when(bookingMapper.toEntity(request)).thenReturn(mappedBooking);
        when(workspaceService.getEntity(10L)).thenReturn(workspace);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toDto(mappedBooking)).thenReturn(response);

        // Act
        BookingDto result = bookingService.create(request);

        // Assert
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());

        assertSame(response, result);
        assertEquals(BookingStatus.CONFIRMED, captor.getValue().getStatus());
        assertEquals(new BigDecimal("62.50"), captor.getValue().getTotalPrice());
    }

    @Test
    @DisplayName("create з NULL запитом викидає IllegalArgumentException")
    void createNullRequestThrowsIllegalArgumentExceptionAndDoesNotMap() {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.create(null)
        );

        // Assert
        assertEquals("Booking data must not be null", exception.getMessage());
        verify(bookingMapper, never()).toEntity(any(BookingDto.class));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("create returns a retryable service-unavailable error when the async queue is full")
    void createRejectedBackgroundTaskThrowsAsyncProcessingUnavailableException() {
        BookingDto request = bookingDto(null, 10L, START, END, BookingStatus.CONFIRMED);
        Booking booking = booking(null, null, START, END, BookingStatus.CONFIRMED);
        BookingDto response = bookingDto(1L, 10L, START, END, BookingStatus.CONFIRMED);
        when(bookingMapper.toEntity(request)).thenReturn(booking);
        when(workspaceService.getEntity(10L)).thenReturn(workspace(10L));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toDto(booking)).thenReturn(response);
        doThrow(new TaskRejectedException("queue full"))
                .when(bookingSideEffectService).processBookingCreated(response);

        AsyncProcessingUnavailableException exception = assertThrows(
                AsyncProcessingUnavailableException.class,
                () -> bookingService.create(request)
        );

        assertEquals("Background booking tasks are busy. Please try again later.", exception.getMessage());
        verify(bookingSideEffectService).processBookingCreated(response);
    }

    @Test
    @DisplayName("create з часом закінчення раніше початку викидає IllegalArgumentException")
    void createEndBeforeStartThrowsIllegalArgumentExceptionAndDoesNotSave() {
        // Arrange
        BookingDto request = bookingDto(null, 10L, END, START, BookingStatus.CONFIRMED);
        Booking mappedBooking = booking(null, null, END, START, BookingStatus.CONFIRMED);
        when(bookingMapper.toEntity(request)).thenReturn(mappedBooking);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.create(request)
        );

        // Assert
        assertEquals("Booking end time must be after start time", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(bookingMapper, never()).toDto(any(Booking.class));
    }

    @Test
    @DisplayName("create з NULL часом початку викидає IllegalArgumentException")
    void createNullStartTimeThrowsIllegalArgumentExceptionAndDoesNotSave() {
        // Arrange
        BookingDto request = bookingDto(null, 10L, null, END, BookingStatus.CONFIRMED);
        Booking mappedBooking = booking(null, null, null, END, BookingStatus.CONFIRMED);
        when(bookingMapper.toEntity(request)).thenReturn(mappedBooking);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.create(request)
        );

        // Assert
        assertEquals("Booking start and end times must not be null", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(bookingMapper, never()).toDto(any(Booking.class));
        verify(workspaceService, never()).getEntity(any());
    }

    @Test
    @DisplayName("create з NULL часом закінчення викидає IllegalArgumentException")
    void createNullEndTimeThrowsIllegalArgumentExceptionAndDoesNotSave() {
        // Arrange
        BookingDto request = bookingDto(null, 10L, START, null, BookingStatus.CONFIRMED);
        Booking mappedBooking = booking(null, null, START, null, BookingStatus.CONFIRMED);
        when(bookingMapper.toEntity(request)).thenReturn(mappedBooking);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.create(request)
        );

        // Assert
        assertEquals("Booking start and end times must not be null", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(bookingMapper, never()).toDto(any(Booking.class));
        verify(workspaceService, never()).getEntity(any());
    }

    @Test
    @DisplayName("update для існуючого бронювання перераховує ціну та повертає DTO")
    void updateExistingBookingRecalculatesTotalPriceAndReturnsMappedDto() {
        // Arrange
        Workspace oldWorkspace = workspace(10L);
        Workspace newWorkspace = workspace(20L);
        Booking existingBooking = booking(1L, oldWorkspace, START, END, BookingStatus.CONFIRMED);
        BookingDto request = bookingDto(1L, 20L, START.plusHours(1), END.plusHours(2), BookingStatus.CANCELLED);
        BookingDto response = bookingDto(1L, 20L, START.plusHours(1), END.plusHours(2), BookingStatus.CANCELLED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(existingBooking));
        doAnswer(invocation -> {
            BookingDto dto = invocation.getArgument(0);
            Booking booking = invocation.getArgument(1);
            booking.setCustomerName(dto.getCustomerName());
            booking.setCustomerEmail(dto.getCustomerEmail());
            booking.setStartsAt(dto.getStartsAt());
            booking.setEndsAt(dto.getEndsAt());
            booking.setStatus(dto.getStatus());
            return null;
        }).when(bookingMapper).updateEntityFromDto(request, existingBooking);
        when(workspaceService.getEntity(20L)).thenReturn(newWorkspace);
        when(bookingMapper.toDto(existingBooking)).thenReturn(response);

        // Act
        BookingDto result = bookingService.update(1L, request);

        // Assert
        assertSame(response, result);
        assertSame(newWorkspace, existingBooking.getWorkspace());
        assertEquals(BookingStatus.CANCELLED, existingBooking.getStatus());
        assertEquals(new BigDecimal("87.50"), existingBooking.getTotalPrice());
        verify(bookingMapper).updateEntityFromDto(request, existingBooking);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("update коли час початку дорівнює часу закінчення викидає IllegalArgumentException")
    void updateEndEqualsStartThrowsIllegalArgumentExceptionAndDoesNotReturnDto() {
        // Arrange
        Workspace workspace = workspace(10L);
        Booking existingBooking = booking(1L, workspace, START, START, BookingStatus.CONFIRMED);
        BookingDto request = bookingDto(1L, 10L, START, START, BookingStatus.CONFIRMED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(existingBooking));

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.update(1L, request)
        );

        // Assert
        assertEquals("Booking end time must be after start time", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(bookingMapper, never()).toDto(any(Booking.class));
    }

    @Test
    @DisplayName("update відсутнього бронювання викидає ResourceNotFoundException")
    void updateMissingBookingThrowsResourceNotFoundExceptionAndDoesNotUpdate() {
        // Arrange
        BookingDto request = bookingDto(404L, 10L, START, END, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(404L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookingService.update(404L, request)
        );

        // Assert
        assertEquals("Booking 404 not found", exception.getMessage());
        verify(bookingMapper, never()).updateEntityFromDto(any(BookingDto.class), any(Booking.class));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("delete існуючого бронювання видаляє сутність")
    void deleteExistingBookingDeletesResolvedEntity() {
        // Arrange
        Booking booking = booking(1L, workspace(10L), START, END, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // Act
        bookingService.delete(1L);

        // Assert
        verify(bookingRepository).findById(1L);
        verify(bookingRepository).delete(booking);
    }

    @Test
    @DisplayName("delete відсутнього бронювання викидає ResourceNotFoundException")
    void deleteMissingBookingThrowsResourceNotFoundExceptionAndDoesNotDelete() {
        // Arrange
        when(bookingRepository.findById(404L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookingService.delete(404L)
        );

        // Assert
        assertEquals("Booking 404 not found", exception.getMessage());
        verify(bookingRepository, never()).delete(any(Booking.class));
    }

    @Test
    @DisplayName("getEntity для існуючого ID повертає сутність")
    void getEntityExistingBookingReturnsEntity() {
        // Arrange
        Booking booking = booking(1L, workspace(10L), START, END, BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // Act
        Booking result = bookingService.getEntity(1L);

        // Assert
        assertSame(booking, result);
        verify(bookingRepository).findById(1L);
    }

    @Test
    @DisplayName("getEntity для відсутнього ID викидає ResourceNotFoundException")
    void getEntityMissingBookingThrowsResourceNotFoundException() {
        // Arrange
        when(bookingRepository.findById(404L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookingService.getEntity(404L)
        );

        // Assert
        assertEquals("Booking 404 not found", exception.getMessage());
        verify(bookingRepository).findById(404L);
    }

    private static Booking booking(
            Long id,
            Workspace workspace,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            BookingStatus status
    ) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setWorkspace(workspace);
        booking.setCustomerName("Alice Cooper");
        booking.setCustomerEmail("alice@example.com");
        booking.setStartsAt(startsAt);
        booking.setEndsAt(endsAt);
        booking.setStatus(status);
        return booking;
    }

    private static BookingDto bookingDto(
            Long id,
            Long workspaceId,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            BookingStatus status
    ) {
        BookingDto dto = new BookingDto();
        dto.setId(id);
        dto.setWorkspaceId(workspaceId);
        dto.setWorkspaceName("Focus Room");
        dto.setCustomerName("Alice Cooper");
        dto.setCustomerEmail("alice@example.com");
        dto.setStartsAt(startsAt);
        dto.setEndsAt(endsAt);
        dto.setStatus(status);
        return dto;
    }

    private static Workspace workspace(Long id) {
        Workspace workspace = new Workspace();
        workspace.setId(id);
        workspace.setName("Focus Room");
        workspace.setType("ROOM");
        workspace.setCapacity(4);
        workspace.setHourlyRate(new BigDecimal("25.00"));
        workspace.setActive(true);
        return workspace;
    }
}
