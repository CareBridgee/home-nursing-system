package iti.jets.java.carenest.exception;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends BusinessException {

    public InvalidOtpException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "INVALID_OTP");
    }
}
