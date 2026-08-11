package iti.jets.java.homenursing.dto.profile;

import java.util.UUID;

public record ProfileMedicalConditionRequest(
        UUID medicalConditionId,
        String name,
        String description
) {
}