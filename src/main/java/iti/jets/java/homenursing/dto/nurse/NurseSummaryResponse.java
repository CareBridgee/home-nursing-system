package iti.jets.java.homenursing.dto.nurse;

import java.math.BigDecimal;
import java.util.UUID;

public record NurseSummaryResponse(
        UUID id,
        String firstName,
        String lastName,
        BigDecimal ratingAvg,
        Integer totalReviews
) {
}
