package iti.jets.java.homenursing.dto.servicerequest;

import jakarta.validation.constraints.NotBlank;

public record CompleteServiceRequestRequest(
        @NotBlank String visitCode
) {
}
