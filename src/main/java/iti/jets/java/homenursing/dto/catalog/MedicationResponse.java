package iti.jets.java.homenursing.dto.catalog;

import iti.jets.java.homenursing.entity.enums.CatalogSource;

import java.time.LocalDateTime;
import java.util.UUID;

public record MedicationResponse(
        UUID id,
        String name,
        CatalogSource source,
        LocalDateTime createdAt
) {
}
