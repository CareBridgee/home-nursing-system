package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.entity.enums.AllergyType;

public record AllergyRequest(
        String name,
        AllergyType type
) {
}
