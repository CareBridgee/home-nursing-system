package iti.jets.java.homenursing.dto.profile;

import iti.jets.java.homenursing.entity.enums.AllergyType;

import java.util.UUID;

public record ProfileAllergyRequest(
        UUID allergyId,
        String name,
        AllergyType type
) {
}