package iti.jets.java.homenursing.dto.nurseoffer;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record NurseOfferUpdateRequest(
        @Positive BigDecimal proposedPrice,
        LocalDate proposedDate,
        LocalTime proposedTime,
        String message
) {
}
