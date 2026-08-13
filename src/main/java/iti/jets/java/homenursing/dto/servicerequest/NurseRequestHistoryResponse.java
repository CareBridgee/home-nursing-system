package iti.jets.java.homenursing.dto.servicerequest;

import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record NurseRequestHistoryResponse(
        UUID serviceRequestId,
        UUID serviceTypeId,
        String serviceName,
        Integer estimatedDurationMinutes,
        UUID patientProfileId,
        String patientFirstName,
        String patientLastName,
        String patientPhoneNumber,
        String patientProfileImageUrl,
        String serviceDescription,
        LocalDate preferredDate,
        LocalTime preferredTime,
        ServiceRequestStatus status,
        BigDecimal estimatedPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
