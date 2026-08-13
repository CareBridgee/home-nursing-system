package iti.jets.java.homenursing.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public ConflictException(String message, Map<String, Object> details) {
        super(message, HttpStatus.CONFLICT, "CONFLICT", details);
    }
}