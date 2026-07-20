package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.entity.enums.AllergyType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProfileAllergyResponse(
        UUID id,
        UUID profileId,
        UUID allergyId,
        String allergyName,
        AllergyType allergyType,
        LocalDateTime createdAt
) {
}
