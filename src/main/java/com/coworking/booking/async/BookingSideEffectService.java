package com.coworking.booking.async;

import com.coworking.booking.dto.BookingDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BookingSideEffectService {

    private final long operationDelayMillis;

    public BookingSideEffectService(
            @Value("${booking.async.side-effect-delay-ms:1000}") long operationDelayMillis
    ) {
        this.operationDelayMillis = operationDelayMillis;
    }

    @Async("bookingTaskExecutor")
    public void processBookingCreated(BookingDto booking) {
        String task = "booking " + booking.getId();
        log.info("Starting background side effects for {} on thread {}", task, Thread.currentThread().getName());

        performSlowOperation("send confirmation email", task);
        performSlowOperation("generate booking receipt PDF", task);
        performSlowOperation("synchronize booking with the access service", task);

        log.info("Finished background side effects for {}", task);
    }

    private void performSlowOperation(String operation, String task) {
        log.info("Starting to {} for {}", operation, task);
        try {
            Thread.sleep(operationDelayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while trying to " + operation + " for " + task, exception);
        }
        log.info("Finished {} for {}", operation, task);
    }
}
