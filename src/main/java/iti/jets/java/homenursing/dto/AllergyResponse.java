package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.entity.enums.AllergyType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AllergyResponse(
        UUID id,
        String name,
        AllergyType type,
        LocalDateTime createdAt
) {
}
