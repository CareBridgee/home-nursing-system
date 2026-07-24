package iti.jets.java.homenursing.dto;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public record ServiceTypeRequest(
        String name,
        String description,
        String category,
        Integer minimumDurationMinutes,
        Integer estimatedDurationMinutes,
        BigDecimal basePrice,
        List<String> includedItems,
        String preparationNote,
        MultipartFile image
) {
}
