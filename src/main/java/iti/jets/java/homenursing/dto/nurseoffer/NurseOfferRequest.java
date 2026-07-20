package iti.jets.java.homenursing.dto.nurseoffer;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record NurseOfferRequest(
        @NotNull UUID nurseId,
        @NotNull UUID serviceRequestId,
        BigDecimal proposedPrice,
        LocalDate proposedDate,
        LocalTime proposedTime,
        String message
) {
}
