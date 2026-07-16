package iti.jets.java.carenest.dto;

import iti.jets.java.carenest.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        Map<String, Object> details
) {
    public static ApiError from(BusinessException ex) {
        return new ApiError(
                Instant.now(),
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getCode(),
                ex.getMessage(),
                ex.getDetails()
        );
    }

    public static ApiError from(HttpStatus status, String code, String message, Map<String, Object> details) {
        return new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                details
        );
    }
}
