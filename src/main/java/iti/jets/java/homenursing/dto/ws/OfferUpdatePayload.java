package iti.jets.java.homenursing.dto.ws;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record OfferUpdatePayload(
        @NotNull UUID offerId,
        BigDecimal proposedPrice,
        LocalDate proposedDate,
        LocalTime proposedTime,
        String message
) {
}