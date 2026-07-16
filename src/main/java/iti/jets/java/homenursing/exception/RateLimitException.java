package iti.jets.java.carenest.exception;

import org.springframework.http.HttpStatus;

public class RateLimitException extends BusinessException {

    public RateLimitException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
    }
}
