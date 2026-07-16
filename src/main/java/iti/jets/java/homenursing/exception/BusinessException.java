package iti.jets.java.homenursing.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    protected BusinessException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", null);
    }

    protected BusinessException(String message, HttpStatus status, String code) {
        this(message, status, code, null);
    }

    protected BusinessException(String message, HttpStatus status, String code, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }
}
