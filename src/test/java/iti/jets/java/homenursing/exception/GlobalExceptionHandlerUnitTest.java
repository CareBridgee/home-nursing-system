package iti.jets.java.homenursing.exception;

import iti.jets.java.homenursing.dto.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
class GlobalExceptionHandlerUnitTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void businessExceptionUsesItsOwnStatusAndCode() {
        ResponseEntity<ApiError> response =
                handler.handleBusinessException(new BadRequestException("bad input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError body = response.getBody();
        assertThat(body.code()).isEqualTo("BAD_REQUEST");
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
        assertThat(body.message()).isEqualTo("bad input");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void methodArgumentNotValidCollectsFieldErrors() {
        var fieldError = new org.springframework.validation.FieldError(
                "target", "phoneNumber", "must not be blank");
        var bindingResult = new org.springframework.validation.BeanPropertyBindingResult(
                new Object(), "target");
        bindingResult.addError(fieldError);
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError body = response.getBody();
        assertThat(body.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.details()).containsEntry("phoneNumber", "must not be blank");
    }

    @Test
    void handlerMethodValidationCollectsParameterErrors() {
        var parameterValidationResult = mock(org.springframework.validation.method.ParameterValidationResult.class);
        var methodParameter = new org.springframework.core.MethodParameter(
                methodWithParameter(), 0);
        when(parameterValidationResult.getMethodParameter()).thenReturn(methodParameter);
        when(parameterValidationResult.getResolvableErrors()).thenReturn(java.util.List.of(
                new org.springframework.validation.FieldError("target", "param", "must not be null")));
        HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
        when(ex.getParameterValidationResults()).thenReturn(java.util.List.of(parameterValidationResult));

        ResponseEntity<ApiError> response = handler.handleHandlerMethodValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().details()).containsEntry("param", "must not be null");
    }

    private static java.lang.reflect.Method methodWithParameter() {
        try {
            return GlobalExceptionHandlerUnitTest.class.getDeclaredMethod("sampleParameter", String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unused")
    private void sampleParameter(String param) {
    }

    @Test
    void malformedBodyIsBadRequestWithInvalidRequestCode() {
        ResponseEntity<ApiError> response =
                handler.handleMalformedBody(mock(HttpMessageNotReadableException.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void maxUploadSizeIsPayloadTooLarge() {
        ResponseEntity<ApiError> response =
                handler.handleMaxUploadSizeExceeded(mock(MaxUploadSizeExceededException.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody().code()).isEqualTo("FILE_TOO_LARGE");
        assertThat(response.getBody().message()).contains("10 MB");
    }

    @Test
    void missingParameterReportsItsMessage() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("q", "String");

        ResponseEntity<ApiError> response = handler.handleMissingParam(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("MISSING_PARAMETER");
        assertThat(response.getBody().message()).contains("q");
    }

    @Test
    void typeMismatchReportsItsMessage() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getMessage()).thenReturn("Failed to convert 'id'");

        ResponseEntity<ApiError> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("TYPE_MISMATCH");
        assertThat(response.getBody().message()).isEqualTo("Failed to convert 'id'");
    }

    @Test
    void noResourceFoundIsNotFound() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getMessage()).thenReturn("No static resource foo");

        ResponseEntity<ApiError> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void methodNotSupportedIs405() {
        HttpRequestMethodNotSupportedException ex = mock(HttpRequestMethodNotSupportedException.class);
        when(ex.getMessage()).thenReturn("Request method 'POST' is not supported");

        ResponseEntity<ApiError> response = handler.handleMethodNotAllowed(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().code()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    void invalidDataAccessIsBadRequest() {
        InvalidDataAccessApiUsageException ex =
                new InvalidDataAccessApiUsageException("sort property 'x' does not exist");

        ResponseEntity<ApiError> response = handler.handleInvalidDataAccess(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_QUERY_PARAMETER");
    }

    @Test
    void unexpectedExceptionIs500WithGenericBody() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(new IllegalStateException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiError body = response.getBody();
        assertThat(body.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.message()).isEqualTo("An unexpected error occurred");
        assertThat(body.details()).isNull();
    }

    @Test
    void dataIntegrityUniqueViolationIsConflictDuplicate() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("dup", new SQLException("duplicate key", "23505"));

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
    }

    @Test
    void dataIntegrityForeignKeyViolationIsConflictInvalidReference() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("fk", new SQLException("foreign key", "23503"));

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("INVALID_REFERENCE");
    }

    @Test
    void dataIntegrityNotNullViolationIs422() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("nn", new SQLException("null value", "23502"));

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().code()).isEqualTo("DATA_INTEGRITY_VIOLATION");
    }

    @Test
    void dataIntegrityCheckViolationIs422() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("ck", new SQLException("check", "23514"));

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().code()).isEqualTo("DATA_INTEGRITY_VIOLATION");
    }

    @Test
    void dataIntegrityOther23StateIsConflict() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("other", new SQLException("other", "23999"));

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("DATA_INTEGRITY_VIOLATION");
    }

    @Test
    void dataIntegrityNon23StateIsInternalError() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("other", new SQLException("other", "08001"));

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("DATA_INTEGRITY_VIOLATION");
    }

    @Test
    void dataIntegrityWithoutSqlStateIsInternalError() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("boom", new RuntimeException("no sql state"));

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("DATA_INTEGRITY_VIOLATION");
    }

    @Test
    void dataIntegrityWithoutCauseIsInternalError() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("boom", (Throwable) null);

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("DATA_INTEGRITY_VIOLATION");
    }

    @Test
    void dataIntegrityWithHibernateConstraintViolationMapsTo23Conflict() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("boom", new ConstraintViolationException());

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("DATA_INTEGRITY_VIOLATION");
    }

    static class ConstraintViolationException extends RuntimeException {
    }
}
