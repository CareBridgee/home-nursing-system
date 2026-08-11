package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.profile.ProfileMedicationRequest;
import iti.jets.java.homenursing.dto.profile.ProfileMedicationResponse;
import iti.jets.java.homenursing.entity.Medication;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ProfileMedication;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ProfileMedicationMapper;
import iti.jets.java.homenursing.repository.MedicationRepository;
import iti.jets.java.homenursing.repository.ProfileMedicationRepository;
import iti.jets.java.homenursing.service.impl.ProfileMedicationServiceImpl;
import iti.jets.java.homenursing.util.CatalogEntryCreator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ProfileMedicationServiceImplTest {

    @Mock
    private ProfileMedicationRepository profileMedicationRepository;
    @Mock
    private ProfileMedicationMapper profileMedicationMapper;
    @Mock
    private ProfileService profileService;
    @Mock
    private MedicationRepository medicationRepository;
    @Mock
    private CatalogEntryCreator catalogEntryCreator;

    @InjectMocks
    private ProfileMedicationServiceImpl profileMedicationService;

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID MEDICATION_ID = UUID.randomUUID();

    private static Medication medication(String name) {
        return Medication.builder().id(MEDICATION_ID).name(name).build();
    }

    private static ProfileMedication profileMedication() {
        return ProfileMedication.builder()
                .id(UUID.randomUUID())
                .profile(Profile.builder().id(PROFILE_ID).build())
                .medication(medication("Metformin"))
                .build();
    }

    @Test
    void listByProfileWithOwnerVerifiesOwnershipAndMaps() {
        ProfileMedication link = profileMedication();
        when(profileMedicationRepository.findByProfileId(PROFILE_ID)).thenReturn(List.of(link));
        when(profileMedicationMapper.toResponse(link)).thenReturn(
                new ProfileMedicationResponse(link.getId(), PROFILE_ID, MEDICATION_ID, "Metformin", null));

        List<ProfileMedicationResponse> responses = profileMedicationService.listByProfile(PROFILE_ID, USER_ID);

        verify(profileService).getOwnedProfileEntity(PROFILE_ID, USER_ID);
        assertThat(responses).singleElement().satisfies(r -> assertThat(r.medicationName()).isEqualTo("Metformin"));
    }

    @Test
    void addToProfileResolvesByMedicationId() {
        Medication medication = medication("Aspirin");
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicationRepository.findById(MEDICATION_ID)).thenReturn(Optional.of(medication));
        when(profileMedicationRepository.existsByProfileIdAndMedicationId(PROFILE_ID, MEDICATION_ID)).thenReturn(false);
        when(profileMedicationRepository.save(any(ProfileMedication.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileMedicationMapper.toResponse(any(ProfileMedication.class))).thenReturn(
                new ProfileMedicationResponse(UUID.randomUUID(), PROFILE_ID, MEDICATION_ID, "Aspirin", null));

        ProfileMedicationResponse response = profileMedicationService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicationRequest(MEDICATION_ID, null));

        assertThat(response.medicationId()).isEqualTo(MEDICATION_ID);
        verify(catalogEntryCreator, never()).createMedication(any());
    }

    @Test
    void addToProfileWithUnknownMedicationIdThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicationRepository.findById(MEDICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileMedicationService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicationRequest(MEDICATION_ID, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medication not found");
    }

    @Test
    void addToProfileWithNameUsesExistingCatalogEntry() {
        Medication medication = medication("Metformin");
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicationRepository.findByNameIgnoreCase("Metformin")).thenReturn(Optional.of(medication));
        when(profileMedicationRepository.existsByProfileIdAndMedicationId(PROFILE_ID, MEDICATION_ID)).thenReturn(false);
        when(profileMedicationRepository.save(any(ProfileMedication.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileMedicationMapper.toResponse(any(ProfileMedication.class))).thenReturn(
                new ProfileMedicationResponse(UUID.randomUUID(), PROFILE_ID, MEDICATION_ID, "Metformin", null));

        ProfileMedicationResponse response = profileMedicationService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicationRequest(null, "  Metformin  "));

        assertThat(response.medicationId()).isEqualTo(MEDICATION_ID);
        verify(medicationRepository).findByNameIgnoreCase("Metformin");
    }

    @Test
    void addToProfileWithNameCreatesNewCatalogEntry() {
        Medication created = medication("Insulin");
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicationRepository.findByNameIgnoreCase("Insulin")).thenReturn(Optional.empty());
        when(catalogEntryCreator.createMedication("Insulin")).thenReturn(created);
        when(profileMedicationRepository.existsByProfileIdAndMedicationId(PROFILE_ID, MEDICATION_ID)).thenReturn(false);
        when(profileMedicationRepository.save(any(ProfileMedication.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileMedicationMapper.toResponse(any(ProfileMedication.class))).thenReturn(
                new ProfileMedicationResponse(UUID.randomUUID(), PROFILE_ID, MEDICATION_ID, "Insulin", null));

        ProfileMedicationResponse response = profileMedicationService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicationRequest(null, "Insulin"));

        assertThat(response.medicationName()).isEqualTo("Insulin");
        verify(catalogEntryCreator).createMedication("Insulin");
    }

    @Test
    void addToProfileWhenCatalogSaveFailsFallsBackToConcurrentEntry() {
        Medication fallback = medication("Metformin");
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicationRepository.findByNameIgnoreCase("Metformin"))
                .thenReturn(Optional.empty(), Optional.of(fallback));
        when(catalogEntryCreator.createMedication("Metformin"))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(profileMedicationRepository.existsByProfileIdAndMedicationId(PROFILE_ID, MEDICATION_ID)).thenReturn(false);
        when(profileMedicationRepository.save(any(ProfileMedication.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileMedicationMapper.toResponse(any(ProfileMedication.class))).thenReturn(
                new ProfileMedicationResponse(UUID.randomUUID(), PROFILE_ID, MEDICATION_ID, "Metformin", null));

        ProfileMedicationResponse response = profileMedicationService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicationRequest(null, "Metformin"));

        assertThat(response.medicationId()).isEqualTo(MEDICATION_ID);
    }

    @Test
    void addToProfileWhenCatalogSaveFailsAndLookupMissingThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicationRepository.findByNameIgnoreCase("Metformin")).thenReturn(Optional.empty());
        when(catalogEntryCreator.createMedication("Metformin"))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(medicationRepository.findByNameIgnoreCase("Metformin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileMedicationService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicationRequest(null, "Metformin")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to create medication");
    }

    @Test
    void addToProfileWithDuplicateLinkThrows() {
        Medication medication = medication("Metformin");
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicationRepository.findByNameIgnoreCase("Metformin")).thenReturn(Optional.of(medication));
        when(profileMedicationRepository.existsByProfileIdAndMedicationId(PROFILE_ID, MEDICATION_ID)).thenReturn(true);

        assertThatThrownBy(() -> profileMedicationService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicationRequest(null, "Metformin")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already linked");
    }

    @Test
    void addToProfileWithBlankNameThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());

        assertThatThrownBy(() -> profileMedicationService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicationRequest(null, "   ")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Provide either medication id or name");
    }

    @Test
    void addToProfileWithNullNameThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());

        assertThatThrownBy(() -> profileMedicationService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicationRequest(null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Provide either medication id or name");
    }

    @Test
    void addToProfileWithTooLongNameThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());

        assertThatThrownBy(() -> profileMedicationService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicationRequest(null, "x".repeat(256))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not exceed 255");
    }

    @Test
    void removeFromProfileDeletesExistingLink() {
        ProfileMedication link = profileMedication();
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(profileMedicationRepository.findByProfileIdAndMedicationId(PROFILE_ID, MEDICATION_ID))
                .thenReturn(Optional.of(link));

        profileMedicationService.removeFromProfile(PROFILE_ID, USER_ID, MEDICATION_ID);

        verify(profileMedicationRepository).delete(link);
    }

    @Test
    void removeFromProfileWhenLinkMissingThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(profileMedicationRepository.findByProfileIdAndMedicationId(PROFILE_ID, MEDICATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileMedicationService.removeFromProfile(PROFILE_ID, USER_ID, MEDICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medication link not found");
    }

    @Test
    void listByProfileWithoutOwnershipCheckVerifiesExistence() {
        ProfileMedication link = profileMedication();
        when(profileService.getProfile(PROFILE_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(profileMedicationRepository.findByProfileId(PROFILE_ID)).thenReturn(List.of(link));
        when(profileMedicationMapper.toResponse(link)).thenReturn(
                new ProfileMedicationResponse(link.getId(), PROFILE_ID, MEDICATION_ID, "Metformin", null));

        List<ProfileMedicationResponse> responses = profileMedicationService.listByProfile(PROFILE_ID);

        assertThat(responses).hasSize(1);
        verify(profileService).getProfile(PROFILE_ID);
    }
}
