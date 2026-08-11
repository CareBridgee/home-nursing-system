package iti.jets.java.homenursing.dto.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ServiceTypeResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        String category,
        Integer minimumDurationMinutes,
        Integer estimatedDurationMinutes,
        BigDecimal basePrice,
        List<String> includedItems,
        String preparationNote,
        LocalDateTime createdAt
) {
}
