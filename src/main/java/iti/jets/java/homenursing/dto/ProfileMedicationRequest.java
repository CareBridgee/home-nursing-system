package iti.jets.java.homenursing.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProfileMedicationRequest(
        @NotNull UUID medicationId
) {
}
