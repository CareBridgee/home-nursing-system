package iti.jets.java.homenursing.dto.nurseoffer;

import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record NurseOfferUpdateRequest(
        BigDecimal proposedPrice,
        LocalDate proposedDate,
        LocalTime proposedTime,
        String message,
        NurseOfferStatus status
) {
}
