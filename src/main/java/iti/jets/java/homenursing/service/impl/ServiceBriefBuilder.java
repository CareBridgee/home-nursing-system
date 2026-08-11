package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.servicerequest.PatientMedicalSummary;
import iti.jets.java.homenursing.service.ProfileService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds a short deterministic brief of the patient's medical profile and the
 * requested service. Used to fill the service request description so nurses
 * see the essential care context without any LLM involvement (the AI assistant
 * never sees patient records).
 */
@Component
public class ServiceBriefBuilder {

    private final ProfileService profileService;
    private final PatientMedicalSummaryAssembler patientMedicalSummaryAssembler;

    public ServiceBriefBuilder(ProfileService profileService,
                               PatientMedicalSummaryAssembler patientMedicalSummaryAssembler) {
        this.profileService = profileService;
        this.patientMedicalSummaryAssembler = patientMedicalSummaryAssembler;
    }

    public String build(UUID profileId, String requestedServiceName) {
        PatientMedicalSummary summary =
                patientMedicalSummaryAssembler.build(profileService.getProfile(profileId), false);

        StringBuilder sb = new StringBuilder("Patient: ");
        boolean described = false;
        int age = summary.dateOfBirth() == null
                ? 0
                : Period.between(summary.dateOfBirth(), LocalDate.now()).getYears();
        String gender = summary.gender() == null ? null : summary.gender().name().toLowerCase();

        if (age > 0) {
            sb.append(age).append("-year-old");
            described = true;
        }
        if (gender != null) {
            if (described) sb.append(' ');
            sb.append(gender);
            described = true;
        }
        if (summary.bloodType() != null && !summary.bloodType().isBlank()) {
            if (described) sb.append(", ");
            sb.append("blood type ").append(summary.bloodType());
            described = true;
        }
        if (!described) {
            sb.append("no profile details recorded");
        }
        sb.append('.');

        appendList(sb, "Conditions", summary.medicalConditions());
        appendList(sb, "Allergies", summary.allergies());
        appendList(sb, "Medications", summary.medications());
        if (requestedServiceName != null && !requestedServiceName.isBlank()) {
            sb.append(" Requested service: ").append(requestedServiceName).append('.');
        }
        return sb.toString();
    }

    private void appendList(StringBuilder sb, String label, List<String> items) {
        if (items.isEmpty()) return;
        String joined = items.stream().distinct().collect(Collectors.joining(", "));
        sb.append(' ').append(label).append(": ").append(joined).append('.');
    }
}
