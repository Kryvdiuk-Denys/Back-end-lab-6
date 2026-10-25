package com.coworking.booking.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class BookingAsyncLookupService {

    private final long operationDelayMillis;

    public BookingAsyncLookupService(
            @Value("${booking.async.lookup-delay-ms:1000}") long operationDelayMillis
    ) {
        this.operationDelayMillis = operationDelayMillis;
    }

    @Async("bookingTaskExecutor")
    public CompletableFuture<Boolean> checkAvailability(Long workspaceId) {
        simulateRemoteCall("workspace availability", workspaceId);
        return CompletableFuture.completedFuture(true);
    }

    @Async("bookingTaskExecutor")
    public CompletableFuture<BigDecimal> requestPriceEstimate(BigDecimal hourlyRate, long durationMinutes) {
        simulateRemoteCall("price estimate", durationMinutes + " minutes");
        BigDecimal price = hourlyRate
                .multiply(BigDecimal.valueOf(durationMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return CompletableFuture.completedFuture(price);
    }

    private void simulateRemoteCall(String operation, Object detail) {
        log.info("Calling simulated external {} for {} on thread {}", operation, detail,
                Thread.currentThread().getName());
        try {
            Thread.sleep(operationDelayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during external " + operation, exception);
        }
    }
}
