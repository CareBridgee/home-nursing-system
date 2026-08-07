package iti.jets.java.homenursing.dto.servicerequest;

import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ServiceRequestNursePreviewResponse(
        UUID serviceRequestId,
        UUID serviceTypeId,
        String serviceName,
        String serviceDescription,
        LocalDate preferredDate,
        LocalTime preferredTime,
        ServiceRequestStatus status,
        BigDecimal estimatedPrice,
        LocalDateTime createdAt,
        PatientMedicalSummary patient
) {
}