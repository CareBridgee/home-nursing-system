package iti.jets.java.homenursing.dto.nurseoffer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record NurseOfferRequest(
        @NotNull UUID serviceRequestId,
        @NotNull @Positive BigDecimal proposedPrice,
        @NotNull LocalDate proposedDate,
        @NotNull LocalTime proposedTime,
        String message
) {
}
