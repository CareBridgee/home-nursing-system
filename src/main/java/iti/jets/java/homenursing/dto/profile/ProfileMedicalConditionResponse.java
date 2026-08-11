package iti.jets.java.homenursing.dto.profile;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProfileMedicalConditionResponse(
        UUID id,
        UUID profileId,
        UUID medicalConditionId,
        String conditionName,
        LocalDateTime createdAt
) {
}
