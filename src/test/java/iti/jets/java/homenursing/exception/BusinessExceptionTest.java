package iti.jets.java.homenursing.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class BusinessExceptionTest {

    @Test
    void messageOnlyConstructorDefaultsToInternalServerError() {
        BusinessException ex = new BusinessException("boom") {
        };

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(ex.getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(ex.getDetails()).isNull();
        assertThat(ex.getMessage()).isEqualTo("boom");
    }

    @Test
    void statusAndCodeConstructor() {
        BusinessException ex = new BusinessException("nope", HttpStatus.BAD_REQUEST, "BAD_REQUEST") {
        };

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("BAD_REQUEST");
        assertThat(ex.getDetails()).isNull();
    }

    @Test
    void fullConstructorCarriesDetails() {
        Map<String, Object> details = Map.of("field", "value");
        BusinessException ex =
                new BusinessException("nope", HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION", details) {
                };

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.getCode()).isEqualTo("VALIDATION");
        assertThat(ex.getDetails()).isEqualTo(details);
        assertThat(ex.getMessage()).isEqualTo("nope");
    }

    @Test
    void concreteExceptionsCarryTheirStatusAndCode() {
        assertContract(new BadRequestException("m"), HttpStatus.BAD_REQUEST, "BAD_REQUEST");
        assertContract(new ConflictException("m"), HttpStatus.CONFLICT, "CONFLICT");
        assertContract(new DuplicateResourceException("m"), HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
        assertContract(new ForbiddenException("m"), HttpStatus.FORBIDDEN, "FORBIDDEN");
        assertContract(new InvalidOtpException("m"), HttpStatus.UNAUTHORIZED, "INVALID_OTP");
        assertContract(new RateLimitException("m"), HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
        assertContract(new ResourceNotFoundException("m"), HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
        assertContract(new UnauthorizedException("m"), HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    private static void assertContract(BusinessException ex, HttpStatus status, String code) {
        assertThat(ex.getStatus()).isEqualTo(status);
        assertThat(ex.getCode()).isEqualTo(code);
        assertThat(ex.getMessage()).isEqualTo("m");
        assertThat(ex.getDetails()).isNull();
    }
}
