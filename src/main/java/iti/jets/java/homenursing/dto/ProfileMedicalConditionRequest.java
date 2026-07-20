package iti.jets.java.homenursing.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProfileMedicalConditionRequest(
        @NotNull UUID medicalConditionId
) {
}
