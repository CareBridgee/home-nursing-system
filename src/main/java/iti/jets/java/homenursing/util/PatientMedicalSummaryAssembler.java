package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.dto.servicerequest.PatientMedicalSummary;
import iti.jets.java.homenursing.entity.EmergencyContact;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.repository.EmergencyContactRepository;
import iti.jets.java.homenursing.util.ProfileImageUtil;
import iti.jets.java.homenursing.repository.MedicalHistoryRepository;
import iti.jets.java.homenursing.repository.ProfileAllergyRepository;
import iti.jets.java.homenursing.repository.ProfileMedicalConditionRepository;
import iti.jets.java.homenursing.repository.ProfileMedicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PatientMedicalSummaryAssembler {

    private final ProfileAllergyRepository profileAllergyRepository;
    private final ProfileMedicationRepository profileMedicationRepository;
    private final ProfileMedicalConditionRepository profileMedicalConditionRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final EmergencyContactRepository emergencyContactRepository;

    @Transactional(readOnly = true)
    public PatientMedicalSummary build(Profile profile, boolean includeContactNumbers) {
        List<String> allergies = profileAllergyRepository.findByProfileId(profile.getId()).stream()
                .map(pa -> pa.getAllergy().getName())
                .toList();

        List<String> conditions = profileMedicalConditionRepository.findByProfileId(profile.getId()).stream()
                .map(pc -> pc.getMedicalCondition().getName())
                .toList();

        List<String> medications = profileMedicationRepository.findByProfileId(profile.getId()).stream()
                .map(pm -> pm.getMedication().getName())
                .toList();

        List<PatientMedicalSummary.MedicalHistoryItem> history = medicalHistoryRepository
                .findByProfileIdOrderByCreatedAtDesc(profile.getId()).stream()
                .map(h -> new PatientMedicalSummary.MedicalHistoryItem(
                        h.getType() == null ? null : h.getType().name(),
                        h.getDescription()))
                .toList();

        List<PatientMedicalSummary.EmergencyContactItem> emergencyContacts = emergencyContactRepository
                .findByProfileId(profile.getId()).stream()
                .map(c -> toEmergencyContactItem(c, includeContactNumbers))
                .toList();

        return new PatientMedicalSummary(
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                ProfileImageUtil.resolveProfileImageUrl(profile),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getBloodType(),
                profile.getHeight(),
                profile.getWeight(),
                profile.getMobilityStatus(),
                profile.getMobilityNotes(),
                profile.getPreviousSurgeries(),
                profile.getPreviousHospitalizations(),
                allergies,
                conditions,
                medications,
                history,
                emergencyContacts);
    }

    private PatientMedicalSummary.EmergencyContactItem toEmergencyContactItem(
            EmergencyContact contact, boolean includeContactNumbers) {
        return new PatientMedicalSummary.EmergencyContactItem(
                contact.getContactName(),
                contact.getRelationship(),
                includeContactNumbers ? contact.getPhoneNumber() : null);
    }
}