package com.coworking.booking.async;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AsyncProcessingUnavailableException extends RuntimeException {

    public AsyncProcessingUnavailableException(String message) {
        super(message);
    }

    public AsyncProcessingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
