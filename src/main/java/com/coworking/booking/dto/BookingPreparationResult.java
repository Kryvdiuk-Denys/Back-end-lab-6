package com.coworking.booking.dto;

import java.math.BigDecimal;

public record BookingPreparationResult(
        Boolean workspaceAvailable,
        BigDecimal estimatedPrice,
        String status,
        String message
) {

    public static BookingPreparationResult ready(boolean available, BigDecimal price) {
        return new BookingPreparationResult(available, price, "READY", "Availability and price checks completed.");
    }

    public static BookingPreparationResult failed() {
        return new BookingPreparationResult(
                null,
                null,
                "CHECK_FAILED",
                "An asynchronous check failed. Please retry the preparation request."
        );
    }
}
