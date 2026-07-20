package iti.jets.java.homenursing.dto.nurseoffer;

import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record NurseOfferResponse(
        UUID id,
        UUID serviceRequestId,
        UUID nurseId,
        BigDecimal proposedPrice,
        LocalDate proposedDate,
        LocalTime proposedTime,
        String message,
        NurseOfferStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
