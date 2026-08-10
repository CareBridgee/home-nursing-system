package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.*;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileReportServiceImpl implements ProfileReportService {

    private static final String REPORT_SYSTEM_PROMPT = """
            You are a clinical documentation assistant for a home nursing platform.
            Generate a concise, structured report for the assigned nurse based ONLY on
            the data given below. Never invent facts. If a section has no data, write "Not recorded".

            Structure the report as:
            1. Patient Overview
            2. Medical History
            3. Allergies
            4. Current Medications
            5. Care Considerations for the Nurse
            6. Risk Flags (e.g. conflicting meds, severe allergies, chronic conditions needing monitoring)
            """;

    private final ChatClient reportChatClient;
    private final ProfileService profileService;
    private final MedicalHistoryService medicalHistoryService;
    private final ProfileMedicalConditionService profileMedicalConditionService;
    private final ProfileAllergyService profileAllergyService;
    private final ProfileMedicationService profileMedicationService;

    @Override
    @Transactional(readOnly = true)
    public String generateReport(UUID profileId) {
        // Existence check only — throws ResourceNotFoundException if the profile
        // doesn't exist or is soft-deleted. No ownership/assignment check.
        Profile profile = profileService.getProfile(profileId);

        List<MedicalHistoryResponse> history = medicalHistoryService.listByProfile(profileId);
        List<ProfileMedicalConditionResponse> conditions = profileMedicalConditionService.listByProfile(profileId);
        List<ProfileAllergyResponse> allergies = profileAllergyService.listByProfile(profileId);
        List<ProfileMedicationResponse> medications = profileMedicationService.listByProfile(profileId);

        String context = buildContext(profile, history, conditions, allergies, medications);

        return reportChatClient.prompt()
                .system(REPORT_SYSTEM_PROMPT)
                .user(context)
                .call()
                .content();
    }

    private String buildContext(Profile profile,
                                List<MedicalHistoryResponse> history,
                                List<ProfileMedicalConditionResponse> conditions,
                                List<ProfileAllergyResponse> allergies,
                                List<ProfileMedicationResponse> medications) {
        StringBuilder sb = new StringBuilder();
        sb.append("PATIENT PROFILE:\n");
        sb.append("Name: ").append(profile.getFirstName()).append(" ").append(profile.getLastName()).append("\n");
        sb.append("Date of Birth: ").append(profile.getDateOfBirth()).append("\n");
        sb.append("Gender: ").append(profile.getGender()).append("\n");
        sb.append("Blood Type: ").append(profile.getBloodType()).append("\n");
        sb.append("Height: ").append(profile.getHeight()).append("\n");
        sb.append("Weight: ").append(profile.getWeight()).append("\n");
        sb.append("Mobility Status: ").append(profile.getMobilityStatus()).append("\n");
        sb.append("Mobility Notes: ").append(profile.getMobilityNotes()).append("\n");
        sb.append("Previous Surgeries: ").append(profile.getPreviousSurgeries()).append("\n");
        sb.append("Previous Hospitalizations: ").append(profile.getPreviousHospitalizations()).append("\n\n");

        sb.append("MEDICAL HISTORY:\n");
        history.forEach(h -> sb.append("- ").append(h).append("\n"));
        sb.append("\nMEDICAL CONDITIONS:\n");
        conditions.forEach(c -> sb.append("- ").append(c).append("\n"));
        sb.append("\nALLERGIES:\n");
        allergies.forEach(a -> sb.append("- ").append(a).append("\n"));
        sb.append("\nMEDICATIONS:\n");
        medications.forEach(m -> sb.append("- ").append(m).append("\n"));
        return sb.toString();
    }
}