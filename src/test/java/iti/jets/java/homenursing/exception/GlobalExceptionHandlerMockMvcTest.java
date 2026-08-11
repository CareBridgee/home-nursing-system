package iti.jets.java.homenursing.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
class GlobalExceptionHandlerMockMvcTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void businessExceptionIsMappedWithStatusAndCode() throws Exception {
        mockMvc.perform(get("/probe/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value("bad input"));
    }

    @Test
    void bodyValidationFailureIsMappedWithFieldDetails() throws Exception {
        mockMvc.perform(post("/probe/validated")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.name").value("must not be blank"));
    }

    @Test
    void parameterValidationFailureIsMappedWithParameterDetails() throws Exception {
        mockMvc.perform(get("/probe/constrained")
                        .param("q", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.q").value("must not be blank"));
    }

    @Test
    void malformedJsonIsMappedAsInvalidRequest() throws Exception {
        mockMvc.perform(post("/probe/validated")
                        .contentType("application/json")
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void missingRequiredParameterIsMapped() throws Exception {
        mockMvc.perform(get("/probe/required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
    }

    @Test
    void typeMismatchIsMapped() throws Exception {
        mockMvc.perform(get("/probe/typed").param("id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));
    }

    @Test
    void unsupportedMethodIsMappedTo405() throws Exception {
        mockMvc.perform(post("/probe/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unexpectedExceptionIsMappedTo500() throws Exception {
        mockMvc.perform(get("/probe/explode"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @RestController
    @RequestMapping("/probe")
    static class ProbeController {

        @GetMapping("/business")
        public ResponseEntity<Void> business() {
            throw new BadRequestException("bad input");
        }

        @PostMapping("/validated")
        public ResponseEntity<Void> validated(@Valid @RequestBody ProbeBody body) {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/constrained")
        public ResponseEntity<Void> constrained(@RequestParam @NotBlank String q) {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/required")
        public ResponseEntity<Void> required(@RequestParam String q) {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/typed")
        public ResponseEntity<Void> typed(@RequestParam int id) {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/path/{id}")
        public ResponseEntity<Void> path(@PathVariable String id) {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/explode")
        public ResponseEntity<Void> explode() {
            throw new IllegalStateException("boom");
        }
    }

    static class ProbeBody {
        @NotBlank
        public String name;
    }
}
