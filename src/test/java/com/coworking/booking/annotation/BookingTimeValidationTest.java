package com.coworking.booking.annotation;

import com.coworking.booking.dto.BookingDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingTimeValidationTest {

    @Test
    void whenValidBookingDates_thenNoViolations() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            // GIVEN: Коректні дані (Acceptance criteria 2)
            BookingDto dto = new BookingDto();
            dto.setWorkspaceId(1L);
            dto.setCustomerName("Test User");
            dto.setCustomerEmail("test@example.com");
            dto.setStartsAt(LocalDateTime.now().plusDays(1));
            dto.setEndsAt(LocalDateTime.now().plusDays(2)); // endsAt пізніше за startsAt

            // WHEN: Валідація DTO
            Set<ConstraintViolation<BookingDto>> violations = validator.validate(dto);

            // THEN: Немає жодного порушення
            assertTrue(violations.isEmpty(), "Не повинно бути порушень для валідних дат");
        }
    }

    @Test
    void whenInvalidBookingDates_thenViolationWithSpecificMessage() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            // GIVEN: Некоректні дані (Acceptance criteria 3)
            BookingDto dto = new BookingDto();
            dto.setWorkspaceId(1L);
            dto.setCustomerName("Test User");
            dto.setCustomerEmail("test@example.com");
            dto.setStartsAt(LocalDateTime.now().plusDays(2));
            dto.setEndsAt(LocalDateTime.now().plusDays(1)); // endsAt РАНІШЕ за startsAt

            // WHEN: Валідація DTO
            Set<ConstraintViolation<BookingDto>> violations = validator.validate(dto);

            // THEN: Виникає порушення з чітким повідомленням
            assertFalse(violations.isEmpty());

            ConstraintViolation<BookingDto> violation = violations.stream()
                    .filter(v -> v.getPropertyPath().toString().equals("endsAt"))
                    .findFirst()
                    .orElseThrow();

            assertEquals("Час початку бронювання має бути раніше за час закінчення", violation.getMessage());
        }
    }
}