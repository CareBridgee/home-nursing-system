package iti.jets.java.homenursing.exception;

import iti.jets.java.homenursing.dto.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.core.MethodParameter;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void handlerMethodValidation_withUnknownParameterName_usesRequestKey() {
        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.getParameterName()).thenReturn(null);
        ParameterValidationResult result = mock(ParameterValidationResult.class);
        when(result.getMethodParameter()).thenReturn(methodParameter);
        org.springframework.context.MessageSourceResolvable resolvable = mock(org.springframework.context.MessageSourceResolvable.class);
        when(resolvable.getDefaultMessage()).thenReturn("must not be null");
        when(result.getResolvableErrors()).thenReturn(List.of(resolvable));
        HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
        when(exception.getParameterValidationResults()).thenReturn(List.of(result));

        ResponseEntity<ApiError> response = new GlobalExceptionHandler().handleHandlerMethodValidation(exception);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody().details(), hasKey("request"));
    }
}