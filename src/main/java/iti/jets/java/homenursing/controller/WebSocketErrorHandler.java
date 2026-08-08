package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.ws.SocketErrorPayload;
import iti.jets.java.homenursing.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.security.Principal;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class WebSocketErrorHandler {

    @MessageExceptionHandler(SecurityException.class)
    @SendToUser("/queue/errors")
    public SocketErrorPayload handleSecurity(SecurityException ex, Principal principal) {
        log.warn("WS operation rejected: user={}, reason={}", principalName(principal), ex.getMessage());
        return SocketErrorPayload.of("FORBIDDEN", ex.getMessage());
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public SocketErrorPayload handleBusiness(BusinessException ex, Principal principal) {
        log.warn("WS operation failed: user={}, code={}, reason={}",
                principalName(principal), ex.getCode(), ex.getMessage());
        return SocketErrorPayload.of(ex.getCode(), ex.getMessage());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public SocketErrorPayload handleValidation(MethodArgumentNotValidException ex, Principal principal) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("WS validation failed: user={}, detail={}", principalName(principal), detail);
        return SocketErrorPayload.of("VALIDATION", detail.isBlank() ? "Invalid payload" : detail);
    }

    @MessageExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MessageConversionException.class
    })
    @SendToUser("/queue/errors")
    public SocketErrorPayload handlePayloadError(Exception ex, Principal principal) {
        log.warn("WS payload conversion failed: user={}, reason={}",
                principalName(principal), ex.getMessage());
        return SocketErrorPayload.of("VALIDATION", "Invalid payload: " + ex.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public SocketErrorPayload handleOther(Exception ex, Principal principal) {
        log.error("WS handler exception: user={}", principalName(principal), ex);
        return SocketErrorPayload.of("INTERNAL", "Unexpected server error");
    }

    private static String principalName(Principal principal) {
        return principal != null ? principal.getName() : "?";
    }
}