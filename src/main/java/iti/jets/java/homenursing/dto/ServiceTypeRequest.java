package iti.jets.java.homenursing.dto;

import java.math.BigDecimal;

public record ServiceTypeRequest(
        String name,
        String description,
        Integer estimatedDurationMinutes,
        BigDecimal basePrice
) {
}
