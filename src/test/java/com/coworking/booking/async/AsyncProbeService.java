package com.coworking.booking.async;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class AsyncProbeService {

    @Async("bookingTaskExecutor")
    public CompletableFuture<String> blockUntilReleased(CountDownLatch release, CountDownLatch started) {
        started.countDown();
        await(release);
        return CompletableFuture.completedFuture(Thread.currentThread().getName());
    }

    @Async("bookingTaskExecutor")
    public CompletableFuture<String> meetAtBarrier(CountDownLatch bothStarted) {
        bothStarted.countDown();
        try {
            if (!bothStarted.await(1, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Both tasks did not start concurrently");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted at the test barrier", exception);
        }
        return CompletableFuture.completedFuture(Thread.currentThread().getName());
    }

    @Async("bookingTaskExecutor")
    public CompletableFuture<String> fail() {
        throw new IllegalStateException("expected async failure");
    }

    @Async("bookingTaskExecutor")
    public void failVoid(CountDownLatch started) {
        started.countDown();
        throw new IllegalStateException("expected void async failure");
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the test latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the test latch", exception);
        }
    }
}
