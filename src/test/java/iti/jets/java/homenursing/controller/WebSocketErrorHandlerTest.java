package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.ws.SocketErrorPayload;
import iti.jets.java.homenursing.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.validation.BindingResult;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketErrorHandlerTest {

    private static final java.security.Principal PRINCIPAL = () -> "user-1";

    private final WebSocketErrorHandler handler = new WebSocketErrorHandler();

    private static final class TestBusinessException extends BusinessException {
        TestBusinessException() {
            super("no such offer", HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
        }
    }

    @Test
    void securityException_mapsToForbiddenPayload() {
        SocketErrorPayload payload = handler.handleSecurity(new SecurityException("denied"), PRINCIPAL);
        assertThat(payload.code(), is("FORBIDDEN"));
        assertThat(payload.message(), is("denied"));
    }

    @Test
    void businessException_passesCodeThrough() {
        SocketErrorPayload payload = handler.handleBusiness(new TestBusinessException(), PRINCIPAL);
        assertThat(payload.code(), is("RESOURCE_NOT_FOUND"));
        assertThat(payload.message(), is("no such offer"));
    }

    @Test
    void validation_withBlankDetail_usesDefaultMessage() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        SocketErrorPayload payload = handler.handleValidation(exception, PRINCIPAL);
        assertThat(payload.code(), is("VALIDATION"));
        assertThat(payload.message(), is("Invalid payload"));
    }

    @Test
    void payloadConversion_mapsToValidationPayload_withAnonymousPrincipal() {
        SocketErrorPayload payload =
                handler.handlePayloadError(new MessageConversionException("cannot read payload"), null);
        assertThat(payload.code(), is("VALIDATION"));
        assertThat(payload.message(), containsString("Invalid payload: cannot read payload"));
    }

    @Test
    void unexpectedException_mapsToInternalPayload() {
        SocketErrorPayload payload = handler.handleOther(new IllegalStateException("boom"), PRINCIPAL);
        assertThat(payload.code(), is("INTERNAL"));
        assertThat(payload.message(), is("Unexpected server error"));
    }
}