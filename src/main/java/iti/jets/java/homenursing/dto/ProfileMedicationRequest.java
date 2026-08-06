package iti.jets.java.homenursing.dto;

import java.util.UUID;

public record ProfileMedicationRequest(
        UUID medicationId,
        String name
) {
}