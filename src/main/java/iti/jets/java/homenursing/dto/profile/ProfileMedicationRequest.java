package iti.jets.java.homenursing.dto.profile;

import java.util.UUID;

public record ProfileMedicationRequest(
        UUID medicationId,
        String name
) {
}