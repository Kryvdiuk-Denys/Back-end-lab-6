package com.coworking.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookingPreparationRequest(
        @NotNull Long workspaceId,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt
) {
}
