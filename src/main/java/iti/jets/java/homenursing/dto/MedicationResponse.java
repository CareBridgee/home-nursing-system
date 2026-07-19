package iti.jets.java.homenursing.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MedicationResponse(
        UUID id,
        String name,
        String description,
        LocalDateTime createdAt
) {
}
