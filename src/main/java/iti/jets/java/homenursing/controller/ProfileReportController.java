package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.*;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles/report")
public class ProfileReportController {

    private final ChatClient reportChatClient;
    private final ProfileService profileService;
    private final MedicalHistoryService medicalHistoryService;
    private final ProfileMedicalConditionService profileMedicalConditionService;
    private final ProfileAllergyService profileAllergyService;
    private final ProfileMedicationService profileMedicationService;

    public ProfileReportController(ChatClient reportChatClient,
                                   ProfileService profileService,
                                   MedicalHistoryService medicalHistoryService,
                                   ProfileMedicalConditionService profileMedicalConditionService,
                                   ProfileAllergyService profileAllergyService,
                                   ProfileMedicationService profileMedicationService) {
        this.reportChatClient = reportChatClient;
        this.profileService = profileService;
        this.medicalHistoryService = medicalHistoryService;
        this.profileMedicalConditionService = profileMedicalConditionService;
        this.profileAllergyService = profileAllergyService;
        this.profileMedicationService = profileMedicationService;
    }

    public record ReportResponse(UUID profileId, String report) {}

    @GetMapping("/{profileId}/report")
    public ReportResponse generateReport(@PathVariable UUID profileId) {
        UUID userId = SecurityUtils.currentUserId(); // confirm actual accessor

        ProfileResponse profile = profileService.getOwnedProfile(profileId, userId);
        List<MedicalHistoryResponse> history = medicalHistoryService.listByProfile(profileId, userId);
        List<ProfileMedicalConditionResponse> conditions = profileMedicalConditionService.listByProfile(profileId, userId);
        List<ProfileAllergyResponse> allergies = profileAllergyService.listByProfile(profileId, userId);
        List<ProfileMedicationResponse> medications = profileMedicationService.listByProfile(profileId, userId);

        String context = buildContext(profile, history, conditions, allergies, medications);

        String report = reportChatClient.prompt()
                .system("""
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
                    """)
                .user(context)
                .call()
                .content();

        return new ReportResponse(profileId, report);
    }

    private String buildContext(ProfileResponse profile,
                                List<MedicalHistoryResponse> history,
                                List<ProfileMedicalConditionResponse> conditions,
                                List<ProfileAllergyResponse> allergies,
                                List<ProfileMedicationResponse> medications) {
        StringBuilder sb = new StringBuilder();
        sb.append("PATIENT PROFILE:\n").append(profile).append("\n\n");
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