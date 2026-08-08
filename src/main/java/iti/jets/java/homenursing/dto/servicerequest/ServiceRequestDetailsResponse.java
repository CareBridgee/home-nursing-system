package iti.jets.java.homenursing.dto.servicerequest;

import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ServiceRequestDetailsResponse(
        UUID serviceRequestId,
        ServiceTypeSummary serviceType,
        ProfileSummary profile,
        NurseSummary nurse,
        String serviceDescription,
        LocalDate preferredDate,
        LocalTime preferredTime,
        Integer durationMinutes,
        ServiceRequestStatus status,
        BigDecimal latitude,
        BigDecimal longitude,
        Double distanceKm,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<NurseOfferResponse> offers
) {

    public record ServiceTypeSummary(
            UUID id,
            String name,
            BigDecimal basePrice
    ) {
    }

    public record ProfileSummary(
            UUID id,
            String firstName,
            String lastName,
            String phoneNumber,
            String profileImageUrl
    ) {
    }

    public record NurseSummary(
            UUID id,
            String firstName,
            String lastName,
            String phoneNumber,
            String profileImageUrl,
            BigDecimal ratingAvg,
            Integer totalReviews
    ) {
    }
}