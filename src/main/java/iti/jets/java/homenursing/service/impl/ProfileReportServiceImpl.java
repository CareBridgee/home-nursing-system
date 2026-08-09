package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.*;
import iti.jets.java.homenursing.mapper.MedicalHistoryMapper;
import iti.jets.java.homenursing.mapper.ProfileAllergyMapper;
import iti.jets.java.homenursing.mapper.ProfileMedicalConditionMapper;
import iti.jets.java.homenursing.mapper.ProfileMedicationMapper;
import iti.jets.java.homenursing.repository.MedicalHistoryRepository;
import iti.jets.java.homenursing.repository.ProfileAllergyRepository;
import iti.jets.java.homenursing.repository.ProfileMedicalConditionRepository;
import iti.jets.java.homenursing.repository.ProfileMedicationRepository;
import iti.jets.java.homenursing.service.ProfileReportService;
import iti.jets.java.homenursing.service.ProfileService;
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
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final MedicalHistoryMapper medicalHistoryMapper;
    private final ProfileMedicalConditionRepository profileMedicalConditionRepository;
    private final ProfileMedicalConditionMapper profileMedicalConditionMapper;
    private final ProfileAllergyRepository profileAllergyRepository;
    private final ProfileAllergyMapper profileAllergyMapper;
    private final ProfileMedicationRepository profileMedicationRepository;
    private final ProfileMedicationMapper profileMedicationMapper;

    @Override
    @Transactional(readOnly = true)
    public String generateReport(UUID profileId, UUID userId) {
        ProfileResponse profile = profileService.getAccessibleProfile(profileId, userId);
        List<MedicalHistoryResponse> history = medicalHistoryRepository.findByProfileIdOrderByCreatedAtDesc(profileId)
                .stream()
                .map(medicalHistoryMapper::toResponse)
                .toList();
        List<ProfileMedicalConditionResponse> conditions = profileMedicalConditionRepository.findByProfileId(profileId)
                .stream()
                .map(profileMedicalConditionMapper::toResponse)
                .toList();
        List<ProfileAllergyResponse> allergies = profileAllergyRepository.findByProfileId(profileId).stream()
                .map(profileAllergyMapper::toResponse)
                .toList();
        List<ProfileMedicationResponse> medications = profileMedicationRepository.findByProfileId(profileId).stream()
                .map(profileMedicationMapper::toResponse)
                .toList();

        String context = buildContext(profile, history, conditions, allergies, medications);

        return reportChatClient.prompt()
                .system(REPORT_SYSTEM_PROMPT)
                .user(context)
                .call()
                .content();
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
