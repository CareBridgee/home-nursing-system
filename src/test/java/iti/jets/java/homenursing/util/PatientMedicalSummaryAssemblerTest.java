package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.dto.servicerequest.PatientMedicalSummary;
import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.entity.EmergencyContact;
import iti.jets.java.homenursing.entity.MedicalCondition;
import iti.jets.java.homenursing.entity.MedicalHistory;
import iti.jets.java.homenursing.entity.Medication;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ProfileAllergy;
import iti.jets.java.homenursing.entity.ProfileMedicalCondition;
import iti.jets.java.homenursing.entity.ProfileMedication;
import iti.jets.java.homenursing.entity.enums.Gender;
import iti.jets.java.homenursing.entity.enums.MedicalHistoryType;
import iti.jets.java.homenursing.repository.EmergencyContactRepository;
import iti.jets.java.homenursing.repository.MedicalHistoryRepository;
import iti.jets.java.homenursing.repository.ProfileAllergyRepository;
import iti.jets.java.homenursing.repository.ProfileMedicalConditionRepository;
import iti.jets.java.homenursing.repository.ProfileMedicationRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PatientMedicalSummaryAssemblerTest {

    @Mock
    private ProfileAllergyRepository profileAllergyRepository;
    @Mock
    private ProfileMedicationRepository profileMedicationRepository;
    @Mock
    private ProfileMedicalConditionRepository profileMedicalConditionRepository;
    @Mock
    private MedicalHistoryRepository medicalHistoryRepository;
    @Mock
    private EmergencyContactRepository emergencyContactRepository;

    @InjectMocks
    private PatientMedicalSummaryAssembler assembler;

    private static Profile profile(UUID id) {
        return Profile.builder()
                .id(id)
                .firstName("Mona")
                .lastName("Hassan")
                .dateOfBirth(LocalDate.of(1975, 3, 10))
                .gender(Gender.FEMALE)
                .bloodType("A+")
                .height(new BigDecimal("160"))
                .weight(new BigDecimal("65"))
                .mobilityStatus("Walker")
                .mobilityNotes("Uses walker indoors")
                .previousSurgeries("Appendix")
                .previousHospitalizations("2020")
                .profileImageUrl("https://cdn.example.com/mona.png")
                .build();
    }

    private static ProfileAllergy allergyEntry(String name) {
        return ProfileAllergy.builder()
                .allergy(Allergy.builder().name(name).build())
                .build();
    }

    private static ProfileMedicalCondition conditionEntry(String name) {
        return ProfileMedicalCondition.builder()
                .medicalCondition(MedicalCondition.builder().name(name).build())
                .build();
    }

    private static ProfileMedication medicationEntry(String name) {
        return ProfileMedication.builder()
                .medication(Medication.builder().name(name).build())
                .build();
    }

    private static EmergencyContact contact(String name, String relationship, String phone) {
        return EmergencyContact.builder()
                .contactName(name)
                .relationship(relationship)
                .phoneNumber(phone)
                .build();
    }

    @Test
    void buildAssemblesAllSectionsFromRepositories() {
        UUID profileId = UUID.randomUUID();
        Profile profile = profile(profileId);
        when(profileAllergyRepository.findByProfileId(profileId))
                .thenReturn(List.of(allergyEntry("Penicillin"), allergyEntry("Dust")));
        when(profileMedicalConditionRepository.findByProfileId(profileId))
                .thenReturn(List.of(conditionEntry("Diabetes")));
        when(profileMedicationRepository.findByProfileId(profileId))
                .thenReturn(List.of(medicationEntry("Metformin")));
        when(medicalHistoryRepository.findByProfileIdOrderByCreatedAtDesc(profileId))
                .thenReturn(List.of(
                        MedicalHistory.builder().type(MedicalHistoryType.SURGERY)
                                .description("Appendectomy").build(),
                        MedicalHistory.builder().type(null).description("Note").build()));
        when(emergencyContactRepository.findByProfileId(profileId))
                .thenReturn(List.of(contact("Omar", "Brother", "+201111111111")));

        PatientMedicalSummary summary = assembler.build(profile, true);

        assertThat(summary.profileId()).isEqualTo(profileId);
        assertThat(summary.firstName()).isEqualTo("Mona");
        assertThat(summary.lastName()).isEqualTo("Hassan");
        assertThat(summary.profileImageUrl()).isEqualTo("https://cdn.example.com/mona.png");
        assertThat(summary.dateOfBirth()).isEqualTo(LocalDate.of(1975, 3, 10));
        assertThat(summary.gender()).isEqualTo(Gender.FEMALE);
        assertThat(summary.bloodType()).isEqualTo("A+");
        assertThat(summary.height()).isEqualByComparingTo("160");
        assertThat(summary.weight()).isEqualByComparingTo("65");
        assertThat(summary.mobilityStatus()).isEqualTo("Walker");
        assertThat(summary.mobilityNotes()).isEqualTo("Uses walker indoors");
        assertThat(summary.previousSurgeries()).isEqualTo("Appendix");
        assertThat(summary.previousHospitalizations()).isEqualTo("2020");
        assertThat(summary.allergies()).containsExactly("Penicillin", "Dust");
        assertThat(summary.medicalConditions()).containsExactly("Diabetes");
        assertThat(summary.medications()).containsExactly("Metformin");
        assertThat(summary.medicalHistory()).hasSize(2);
        assertThat(summary.medicalHistory().get(0).type()).isEqualTo("SURGERY");
        assertThat(summary.medicalHistory().get(0).description()).isEqualTo("Appendectomy");
        assertThat(summary.medicalHistory().get(1).type()).isNull();
        assertThat(summary.medicalHistory().get(1).description()).isEqualTo("Note");
        assertThat(summary.emergencyContacts()).hasSize(1);
        assertThat(summary.emergencyContacts().get(0).name()).isEqualTo("Omar");
        assertThat(summary.emergencyContacts().get(0).relationship()).isEqualTo("Brother");
        assertThat(summary.emergencyContacts().get(0).phoneNumber()).isEqualTo("+201111111111");
    }

    @Test
    void buildWithIncludeContactNumbersFalseMasksPhoneNumbers() {
        UUID profileId = UUID.randomUUID();
        Profile profile = profile(profileId);
        when(profileAllergyRepository.findByProfileId(profileId)).thenReturn(List.of());
        when(profileMedicalConditionRepository.findByProfileId(profileId)).thenReturn(List.of());
        when(profileMedicationRepository.findByProfileId(profileId)).thenReturn(List.of());
        when(medicalHistoryRepository.findByProfileIdOrderByCreatedAtDesc(profileId)).thenReturn(List.of());
        when(emergencyContactRepository.findByProfileId(profileId))
                .thenReturn(List.of(contact("Omar", "Brother", "+201111111111")));

        PatientMedicalSummary summary = assembler.build(profile, false);

        assertThat(summary.emergencyContacts()).hasSize(1);
        assertThat(summary.emergencyContacts().get(0).name()).isEqualTo("Omar");
        assertThat(summary.emergencyContacts().get(0).relationship()).isEqualTo("Brother");
        assertThat(summary.emergencyContacts().get(0).phoneNumber()).isNull();
    }

    @Test
    void buildWithEmptyRepositoriesProducesEmptyLists() {
        UUID profileId = UUID.randomUUID();
        Profile profile = profile(profileId);
        when(profileAllergyRepository.findByProfileId(profileId)).thenReturn(List.of());
        when(profileMedicalConditionRepository.findByProfileId(profileId)).thenReturn(List.of());
        when(profileMedicationRepository.findByProfileId(profileId)).thenReturn(List.of());
        when(medicalHistoryRepository.findByProfileIdOrderByCreatedAtDesc(profileId)).thenReturn(List.of());
        when(emergencyContactRepository.findByProfileId(profileId)).thenReturn(List.of());

        PatientMedicalSummary summary = assembler.build(profile, true);

        assertThat(summary.allergies()).isEmpty();
        assertThat(summary.medicalConditions()).isEmpty();
        assertThat(summary.medications()).isEmpty();
        assertThat(summary.medicalHistory()).isEmpty();
        assertThat(summary.emergencyContacts()).isEmpty();
        assertThat(summary.profileImageUrl()).isEqualTo("https://cdn.example.com/mona.png");
    }

    @Test
    void blankProfileImageUrlBecomesNull() {
        UUID profileId = UUID.randomUUID();
        Profile profile = profile(profileId);
        profile.setProfileImageUrl("   ");
        when(profileAllergyRepository.findByProfileId(profileId)).thenReturn(List.of());
        when(profileMedicalConditionRepository.findByProfileId(profileId)).thenReturn(List.of());
        when(profileMedicationRepository.findByProfileId(profileId)).thenReturn(List.of());
        when(medicalHistoryRepository.findByProfileIdOrderByCreatedAtDesc(profileId)).thenReturn(List.of());
        when(emergencyContactRepository.findByProfileId(profileId)).thenReturn(List.of());

        PatientMedicalSummary summary = assembler.build(profile, true);

        assertThat(summary.profileImageUrl()).isNull();
    }
}
