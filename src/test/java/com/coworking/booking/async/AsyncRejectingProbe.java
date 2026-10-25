package com.coworking.booking.async;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class AsyncRejectingProbe {

    @Async("rejectingExecutor")
    public CompletableFuture<Void> block(CountDownLatch release, CountDownLatch started) {
        started.countDown();
        try {
            if (!release.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the rejection test latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted in the rejection test", exception);
        }
        return CompletableFuture.completedFuture(null);
    }
}
