package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.entity.Address;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.Gender;
import iti.jets.java.homenursing.repository.AddressRepository;
import iti.jets.java.homenursing.repository.EmergencyContactRepository;
import iti.jets.java.homenursing.repository.MedicalHistoryRepository;
import iti.jets.java.homenursing.repository.ProfileAllergyRepository;
import iti.jets.java.homenursing.repository.ProfileMedicalConditionRepository;
import iti.jets.java.homenursing.repository.ProfileMedicationRepository;
import iti.jets.java.homenursing.repository.ProfileRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ProfileCompletionCheckerTest {

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private EmergencyContactRepository emergencyContactRepository;
    @Mock
    private ProfileAllergyRepository profileAllergyRepository;
    @Mock
    private ProfileMedicationRepository profileMedicationRepository;
    @Mock
    private ProfileMedicalConditionRepository profileMedicalConditionRepository;
    @Mock
    private MedicalHistoryRepository medicalHistoryRepository;

    @InjectMocks
    private ProfileCompletionChecker checker;

    private static User completeUser(UUID id) {
        return User.builder()
                .id(id)
                .firstName("Ahmed")
                .lastName("Ali")
                .dateOfBirth(LocalDate.of(1990, 5, 1))
                .gender(Gender.MALE)
                .build();
    }

    private static Profile completeProfile(UUID id) {
        return Profile.builder()
                .id(id)
                .bloodType("O+")
                .height(new BigDecimal("170"))
                .weight(new BigDecimal("75"))
                .mobilityStatus("Independent")
                .build();
    }

    private static Address addressWithCity(String city) {
        return Address.builder().city(city).build();
    }

    private void stubPrimaryProfile(UUID userId, Profile profile) {
        when(profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(userId))
                .thenReturn(Optional.ofNullable(profile));
    }

    private void stubAddress(UUID profileId, Address address) {
        when(addressRepository.findByProfileId(profileId)).thenReturn(Optional.ofNullable(address));
    }

    @Test
    void nullFirstNameIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        user.setFirstName(null);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void blankFirstNameIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        user.setFirstName("   ");
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void placeholderFirstNameUserIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        user.setFirstName("  user  ");
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void placeholderFirstNameNurseIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        user.setFirstName("NURSE");
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingLastNameIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        user.setLastName(null);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingDateOfBirthIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        user.setDateOfBirth(null);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingGenderIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        user.setGender(null);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingPrimaryProfileIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        stubPrimaryProfile(user.getId(), null);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void blankBloodTypeIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        Profile profile = completeProfile(UUID.randomUUID());
        profile.setBloodType(" ");
        stubPrimaryProfile(user.getId(), profile);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingHeightIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        Profile profile = completeProfile(UUID.randomUUID());
        profile.setHeight(null);
        stubPrimaryProfile(user.getId(), profile);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingWeightIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        Profile profile = completeProfile(UUID.randomUUID());
        profile.setWeight(null);
        stubPrimaryProfile(user.getId(), profile);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingMobilityStatusIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        Profile profile = completeProfile(UUID.randomUUID());
        profile.setMobilityStatus(null);
        stubPrimaryProfile(user.getId(), profile);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingAddressIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        Profile profile = completeProfile(UUID.randomUUID());
        stubPrimaryProfile(user.getId(), profile);
        stubAddress(profile.getId(), null);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void addressWithoutCityIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        Profile profile = completeProfile(UUID.randomUUID());
        stubPrimaryProfile(user.getId(), profile);
        stubAddress(profile.getId(), addressWithCity(" "));
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingEmergencyContactIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        Profile profile = completeProfile(UUID.randomUUID());
        stubPrimaryProfile(user.getId(), profile);
        stubAddress(profile.getId(), addressWithCity("Cairo"));
        when(emergencyContactRepository.existsByProfileId(profile.getId())).thenReturn(false);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingAllergyIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        Profile profile = completeProfile(UUID.randomUUID());
        stubPrimaryProfile(user.getId(), profile);
        stubAddress(profile.getId(), addressWithCity("Cairo"));
        when(emergencyContactRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(profileAllergyRepository.existsByProfileId(profile.getId())).thenReturn(false);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingMedicationIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        Profile profile = completeProfile(UUID.randomUUID());
        stubPrimaryProfile(user.getId(), profile);
        stubAddress(profile.getId(), addressWithCity("Cairo"));
        when(emergencyContactRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(profileAllergyRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(profileMedicationRepository.existsByProfileId(profile.getId())).thenReturn(false);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingMedicalConditionIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        Profile profile = completeProfile(UUID.randomUUID());
        stubPrimaryProfile(user.getId(), profile);
        stubAddress(profile.getId(), addressWithCity("Cairo"));
        when(emergencyContactRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(profileAllergyRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(profileMedicationRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(profileMedicalConditionRepository.existsByProfileId(profile.getId())).thenReturn(false);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void missingMedicalHistoryIsIncomplete() {
        User user = completeUser(UUID.randomUUID());
        Profile profile = completeProfile(UUID.randomUUID());
        stubPrimaryProfile(user.getId(), profile);
        stubAddress(profile.getId(), addressWithCity("Cairo"));
        when(emergencyContactRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(profileAllergyRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(profileMedicationRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(profileMedicalConditionRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(medicalHistoryRepository.existsByProfileId(profile.getId())).thenReturn(false);
        assertThat(checker.isUserProfileCompleted(user)).isFalse();
    }

    @Test
    void fullyCompletedProfileReturnsTrue() {
        User user = completeUser(UUID.randomUUID());
        Profile profile = completeProfile(UUID.randomUUID());
        stubPrimaryProfile(user.getId(), profile);
        stubAddress(profile.getId(), addressWithCity("Giza"));
        when(emergencyContactRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(profileAllergyRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(profileMedicationRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(profileMedicalConditionRepository.existsByProfileId(profile.getId())).thenReturn(true);
        when(medicalHistoryRepository.existsByProfileId(profile.getId())).thenReturn(true);
        assertThat(checker.isUserProfileCompleted(user)).isTrue();
    }
}
