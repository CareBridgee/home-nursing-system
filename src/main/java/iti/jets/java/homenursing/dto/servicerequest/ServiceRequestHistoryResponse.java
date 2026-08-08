package iti.jets.java.homenursing.dto.servicerequest;

import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ServiceRequestHistoryResponse(
        UUID serviceRequestId,
        UUID serviceTypeId,
        String serviceName,
        String serviceDescription,
        LocalDate preferredDate,
        LocalTime preferredTime,
        ServiceRequestStatus status,
        UUID nurseId,
        String nurseName,
        String nurseProfileImageUrl,
        Double distanceKm,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}