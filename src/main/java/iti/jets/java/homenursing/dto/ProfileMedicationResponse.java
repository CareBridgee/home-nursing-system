package iti.jets.java.homenursing.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProfileMedicationResponse(
        UUID id,
        UUID profileId,
        UUID medicationId,
        String medicationName,
        LocalDateTime createdAt
) {
}
