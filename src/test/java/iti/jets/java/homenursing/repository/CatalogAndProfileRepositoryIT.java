package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.Address;
import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.entity.EmergencyContact;
import iti.jets.java.homenursing.entity.MedicalCondition;
import iti.jets.java.homenursing.entity.MedicalHistory;
import iti.jets.java.homenursing.entity.Medication;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ProfileAllergy;
import iti.jets.java.homenursing.entity.ProfileMedicalCondition;
import iti.jets.java.homenursing.entity.ProfileMedication;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.AllergyType;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import iti.jets.java.homenursing.entity.enums.Gender;
import iti.jets.java.homenursing.entity.enums.MedicalHistoryType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CatalogAndProfileRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private AllergyRepository allergyRepository;
    @Autowired
    private MedicalConditionRepository medicalConditionRepository;
    @Autowired
    private MedicationRepository medicationRepository;
    @Autowired
    private MedicalHistoryRepository medicalHistoryRepository;
    @Autowired
    private EmergencyContactRepository emergencyContactRepository;
    @Autowired
    private ProfileAllergyRepository profileAllergyRepository;
    @Autowired
    private ProfileMedicalConditionRepository profileMedicalConditionRepository;
    @Autowired
    private ProfileMedicationRepository profileMedicationRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User user(String phone) {
        return userRepository.save(User.builder()
                .phoneNumber(phone)
                .firstName("Mona")
                .lastName("Ali")
                .isDeleted(false)
                .build());
    }

    private Profile profile(User user, boolean primary) {
        return profileRepository.save(Profile.builder()
                .user(user)
                .firstName("Mona")
                .lastName("Ali")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .isPrimary(primary)
                .isDeleted(false)
                .build());
    }

    @Test
    void user_findByPhoneNumber_andExists() {
        User user = user("+201000000001");

        assertEquals(user.getId(), userRepository.findByPhoneNumber("+201000000001").orElseThrow().getId());
        assertTrue(userRepository.existsByPhoneNumber("+201000000001"));
        assertFalse(userRepository.existsByPhoneNumber("+201000000099"));
    }

    @Test
    void user_findByPhoneNumberWithProfiles_loadsProfiles() {
        User user = user("+201000000002");
        Profile primary = profile(user, true);
        Profile secondary = profile(user, false);

        entityManager.flush();
        entityManager.clear();

        User loaded = userRepository.findByPhoneNumberWithProfiles("+201000000002").orElseThrow();

        assertEquals(2, loaded.getProfiles().size());
        assertEquals(primary.getId(), profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(user.getId()).orElseThrow().getId());
        assertEquals(2, profileRepository.findByUserIdAndIsDeletedFalse(user.getId()).size());
        assertEquals(user.getId(), profileRepository.findById(secondary.getId()).orElseThrow().getUser().getId());
    }

    @Test
    void profile_findPrimaryAndActiveProfiles() {
        User user = user("+201000000003");
        Profile primary = profile(user, true);
        profile(user, false);

        assertEquals(primary.getId(), profileRepository
                .findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(user.getId()).orElseThrow().getId());
        assertEquals(2, profileRepository.findByUserIdAndIsDeletedFalse(user.getId()).size());
    }

    @Test
    void address_findByProfileId() {
        User user = user("+201000000004");
        Profile profile = profile(user, true);
        addressRepository.save(Address.builder()
                .profile(profile)
                .street("Main St")
                .city("Cairo")
                .build());

        assertEquals("Main St", addressRepository.findByProfileId(profile.getId()).orElseThrow().getStreet());
        assertTrue(addressRepository.findByProfileId(UUID.randomUUID()).isEmpty());
    }

    @Test
    void allergy_findByNameIgnoreCase_andBySource() {
        allergyRepository.save(Allergy.builder().name("Peanuts")
                .type(AllergyType.FOOD).source(CatalogSource.ADMIN).build());
        allergyRepository.save(Allergy.builder().name("Penicillin")
                .type(AllergyType.DRUG).source(CatalogSource.USER).build());

        assertEquals("Peanuts", allergyRepository.findByNameIgnoreCase("peanuts").orElseThrow().getName());
        assertTrue(allergyRepository.findByNameIgnoreCase("sesame").isEmpty());
        assertEquals(1, allergyRepository.findBySource(CatalogSource.ADMIN).size());
        assertEquals(1, allergyRepository.findBySource(CatalogSource.USER).size());
    }

    @Test
    void medicalCondition_findByNameIgnoreCase_andBySource() {
        medicalConditionRepository.save(MedicalCondition.builder().name("Diabetes")
                .description("Type 2").source(CatalogSource.ADMIN).build());
        medicalConditionRepository.save(MedicalCondition.builder().name("Asthma")
                .source(CatalogSource.USER).build());

        assertEquals("Diabetes", medicalConditionRepository.findByNameIgnoreCase("diabetes").orElseThrow().getName());
        assertTrue(medicalConditionRepository.findByNameIgnoreCase("none").isEmpty());
        assertEquals(1, medicalConditionRepository.findBySource(CatalogSource.USER).size());
    }

    @Test
    void medication_findByNameIgnoreCase_andBySource() {
        medicationRepository.save(Medication.builder().name("Metformin")
                .source(CatalogSource.ADMIN).build());
        medicationRepository.save(Medication.builder().name("Insulin")
                .source(CatalogSource.USER).build());

        assertEquals("Metformin", medicationRepository.findByNameIgnoreCase("mETfOrmin").orElseThrow().getName());
        assertTrue(medicationRepository.findByNameIgnoreCase("aspirin").isEmpty());
        assertEquals(1, medicationRepository.findBySource(CatalogSource.ADMIN).size());
    }

    @Test
    void emergencyContact_allQueries() {
        User user = user("+201000000005");
        Profile profile = profile(user, true);
        EmergencyContact contact = emergencyContactRepository.save(EmergencyContact.builder()
                .profile(profile)
                .contactName("Sara")
                .phoneNumber("+201111111111")
                .relationship("Sister")
                .build());

        assertEquals(1, emergencyContactRepository.findByProfileId(profile.getId()).size());
        assertTrue(emergencyContactRepository.existsByProfileId(profile.getId()));
        assertTrue(emergencyContactRepository.existsByProfile_IdAndPhoneNumber(profile.getId(), "+201111111111"));
        assertFalse(emergencyContactRepository.existsByProfile_IdAndPhoneNumber(profile.getId(), "+209999999999"));
        assertTrue(emergencyContactRepository
                .existsByProfile_IdAndPhoneNumberAndIdNot(profile.getId(), "+201111111111", UUID.randomUUID()));
        assertFalse(emergencyContactRepository
                .existsByProfile_IdAndPhoneNumberAndIdNot(profile.getId(), "+201111111111", contact.getId()));
        assertEquals(contact.getId(),
                emergencyContactRepository.findByProfileId(profile.getId()).get(0).getId());
    }

    @Test
    void medicalHistory_findByProfileIdOrderedByCreatedAtDesc() {
        User user = user("+201000000006");
        Profile profile = profile(user, true);
        medicalHistoryRepository.save(MedicalHistory.builder()
                .profile(profile).type(MedicalHistoryType.SURGERY)
                .description("Older").createdAt(java.time.LocalDateTime.of(2025, 1, 1, 10, 0)).build());
        medicalHistoryRepository.save(MedicalHistory.builder()
                .profile(profile).type(MedicalHistoryType.SURGERY)
                .description("Newer").createdAt(java.time.LocalDateTime.of(2026, 1, 1, 10, 0)).build());

        List<MedicalHistory> histories = medicalHistoryRepository.findByProfileIdOrderByCreatedAtDesc(profile.getId());

        assertEquals(2, histories.size());
        assertEquals("Newer", histories.get(0).getDescription());
        assertTrue(medicalHistoryRepository.existsByProfileId(profile.getId()));
        assertFalse(medicalHistoryRepository.existsByProfileId(UUID.randomUUID()));
    }

    @Test
    void profileAllergy_allQueries() {
        User user = user("+201000000007");
        Profile profile = profile(user, true);
        Allergy allergy = allergyRepository.save(Allergy.builder()
                .name("Eggs").type(AllergyType.FOOD).source(CatalogSource.ADMIN).build());
        ProfileAllergy pa = profileAllergyRepository.save(ProfileAllergy.builder()
                .profile(profile).allergy(allergy).createdAt(java.time.LocalDateTime.now()).build());

        assertEquals(1, profileAllergyRepository.findByProfileId(profile.getId()).size());
        assertTrue(profileAllergyRepository.existsByProfileId(profile.getId()));
        assertTrue(profileAllergyRepository.existsByProfileIdAndAllergyId(profile.getId(), allergy.getId()));
        assertFalse(profileAllergyRepository.existsByProfileIdAndAllergyId(UUID.randomUUID(), allergy.getId()));
        assertEquals(pa.getId(), profileAllergyRepository
                .findByProfileIdAndAllergyId(profile.getId(), allergy.getId()).orElseThrow().getId());
        assertTrue(profileAllergyRepository.findByProfileIdAndAllergyId(profile.getId(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void profileMedicalCondition_allQueries() {
        User user = user("+201000000008");
        Profile profile = profile(user, true);
        MedicalCondition condition = medicalConditionRepository.save(MedicalCondition.builder()
                .name("Hypertension").source(CatalogSource.ADMIN).build());
        profileMedicalConditionRepository.save(ProfileMedicalCondition.builder()
                .profile(profile).medicalCondition(condition)
                .createdAt(java.time.LocalDateTime.now()).build());

        assertEquals(1, profileMedicalConditionRepository.findByProfileId(profile.getId()).size());
        assertTrue(profileMedicalConditionRepository.existsByProfileIdAndMedicalConditionId(profile.getId(), condition.getId()));
        assertFalse(profileMedicalConditionRepository.existsByProfileId(UUID.randomUUID()));
        assertTrue(profileMedicalConditionRepository
                .findByProfileIdAndMedicalConditionId(profile.getId(), condition.getId()).isPresent());
        assertTrue(profileMedicalConditionRepository
                .findByProfileIdAndMedicalConditionId(profile.getId(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void profileMedication_allQueries() {
        User user = user("+201000000009");
        Profile profile = profile(user, true);
        Medication medication = medicationRepository.save(Medication.builder()
                .name("Paracetamol").source(CatalogSource.ADMIN).build());
        profileMedicationRepository.save(ProfileMedication.builder()
                .profile(profile).medication(medication)
                .createdAt(java.time.LocalDateTime.now()).build());

        assertEquals(1, profileMedicationRepository.findByProfileId(profile.getId()).size());
        assertTrue(profileMedicationRepository.existsByProfileIdAndMedicationId(profile.getId(), medication.getId()));
        assertFalse(profileMedicationRepository.existsByProfileIdAndMedicationId(UUID.randomUUID(), medication.getId()));
        assertTrue(profileMedicationRepository
                .findByProfileIdAndMedicationId(profile.getId(), medication.getId()).isPresent());
        assertFalse(profileMedicationRepository.existsByProfileId(UUID.randomUUID()));
    }
}
