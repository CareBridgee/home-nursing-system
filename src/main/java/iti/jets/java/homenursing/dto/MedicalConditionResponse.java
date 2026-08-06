package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.entity.enums.CatalogSource;

import java.time.LocalDateTime;
import java.util.UUID;

public record MedicalConditionResponse(
        UUID id,
        String name,
        String description,
        CatalogSource source,
        LocalDateTime createdAt
) {
}
