package iti.jets.java.homenursing.dto.servicerequest;

import iti.jets.java.homenursing.entity.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientMedicalSummary(
        UUID profileId,
        String firstName,
        String lastName,
        String profileImageUrl,
        LocalDate dateOfBirth,
        Gender gender,
        String bloodType,
        BigDecimal height,
        BigDecimal weight,
        String mobilityStatus,
        String mobilityNotes,
        String previousSurgeries,
        String previousHospitalizations,
        List<String> allergies,
        List<String> medicalConditions,
        List<String> medications,
        List<MedicalHistoryItem> medicalHistory,
        List<EmergencyContactItem> emergencyContacts
) {

    public record MedicalHistoryItem(String type, String description) {
    }

    public record EmergencyContactItem(String name, String relationship, String phoneNumber) {
    }
}