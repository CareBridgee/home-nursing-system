package iti.jets.java.homenursing.dto.servicerequest;

import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record NearbyNurseServiceRequestResponse(
        UUID serviceRequestId,
        UUID profileId,
        UUID serviceTypeId,
        String serviceName,
        String serviceDescription,
        LocalDate preferredDate,
        LocalTime preferredTime,
        ServiceRequestStatus status,
        BigDecimal latitude,
        BigDecimal longitude,
        double distanceKm,
        LocalDateTime createdAt
) {
}
