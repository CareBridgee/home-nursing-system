package iti.jets.java.homenursing.dto.catalog;

import iti.jets.java.homenursing.entity.enums.AllergyType;
import iti.jets.java.homenursing.entity.enums.CatalogSource;

import java.time.LocalDateTime;
import java.util.UUID;

public record AllergyResponse(
        UUID id,
        String name,
        AllergyType type,
        CatalogSource source,
        LocalDateTime createdAt
) {
}
