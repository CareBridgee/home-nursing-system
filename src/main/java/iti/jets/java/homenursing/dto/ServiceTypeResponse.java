package iti.jets.java.homenursing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceTypeResponse(
        UUID id,
        String name,
        String description,
        Integer estimatedDurationMinutes,
        BigDecimal basePrice,
        LocalDateTime createdAt
) {
}
