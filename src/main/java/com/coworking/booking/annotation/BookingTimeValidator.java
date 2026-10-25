package com.coworking.booking.annotation;

import com.coworking.booking.dto.BookingDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// Acceptance criteria 1 (THEN): Це саме той клас валідатора, який Bean Validation знаходить
// і викликає автоматично під капотом завдяки @Constraint(validatedBy = BookingTimeValidator.class).
public class BookingTimeValidator implements ConstraintValidator<ValidBookingTime, BookingDto> {

    @Override
    public boolean isValid(BookingDto dto, ConstraintValidatorContext context) {
        if (dto == null || dto.getStartsAt() == null || dto.getEndsAt() == null) {
            return true;
        }

        // Acceptance criteria 2 (GIVEN / WHEN): Основне бізнес-правило.
        // Перевіряємо валідний стан: чи час початку (startsAt) передує часу закінчення (endsAt).
        // Якщо так, повернеться true і валідація пройде успішно (THEN відсутність constraint violation).
        boolean isValid = dto.getStartsAt().isBefore(dto.getEndsAt());

        if (!isValid) {
            // Acceptance criteria 3 (THEN): Якщо дати невалідні, ми не просто кидаємо помилку,
            // а прив'язуємо violation до конкретного поля "endsAt".
            context.disableDefaultConstraintViolation(); // Вимикаємо стандартну помилку на рівні класу

            // Створюємо нову помилку з нашим повідомленням і вказуємо, що проблема саме в полі endsAt
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("endsAt") // <- Ось ця частина виконує вимогу AC 3
                    .addConstraintViolation();
        }

        return isValid;
    }
}