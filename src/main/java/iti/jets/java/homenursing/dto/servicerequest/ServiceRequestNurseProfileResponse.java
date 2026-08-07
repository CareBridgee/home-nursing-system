package iti.jets.java.homenursing.dto.servicerequest;

import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ServiceRequestNurseProfileResponse(
        UUID serviceRequestId,
        UUID serviceTypeId,
        String serviceName,
        String serviceDescription,
        LocalDate preferredDate,
        LocalTime preferredTime,
        ServiceRequestStatus status,
        BigDecimal estimatedPrice,
        LocalDateTime createdAt,
        PatientMedicalSummary patient,
        String patientPhoneNumber,
        AddressSummary address
) {

    public record AddressSummary(
            String country,
            String city,
            String area,
            String street,
            String buildingNumber,
            String apartmentNumber
    ) {
    }
}