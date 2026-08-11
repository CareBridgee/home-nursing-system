package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.profile.ProfileMedicalConditionRequest;
import iti.jets.java.homenursing.dto.profile.ProfileMedicalConditionResponse;
import iti.jets.java.homenursing.entity.MedicalCondition;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ProfileMedicalCondition;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ProfileMedicalConditionMapper;
import iti.jets.java.homenursing.repository.MedicalConditionRepository;
import iti.jets.java.homenursing.repository.ProfileMedicalConditionRepository;
import iti.jets.java.homenursing.service.impl.ProfileMedicalConditionServiceImpl;
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
class ProfileMedicalConditionServiceImplTest {

    @Mock
    private ProfileMedicalConditionRepository profileMedicalConditionRepository;
    @Mock
    private ProfileMedicalConditionMapper profileMedicalConditionMapper;
    @Mock
    private ProfileService profileService;
    @Mock
    private MedicalConditionRepository medicalConditionRepository;
    @Mock
    private CatalogEntryCreator catalogEntryCreator;

    @InjectMocks
    private ProfileMedicalConditionServiceImpl profileMedicalConditionService;

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONDITION_ID = UUID.randomUUID();

    private static MedicalCondition condition(String name) {
        return MedicalCondition.builder().id(CONDITION_ID).name(name).description("desc").build();
    }

    private static ProfileMedicalCondition profileCondition() {
        return ProfileMedicalCondition.builder()
                .id(UUID.randomUUID())
                .profile(Profile.builder().id(PROFILE_ID).build())
                .medicalCondition(condition("Diabetes"))
                .build();
    }

    @Test
    void listByProfileWithOwnerVerifiesOwnershipAndMaps() {
        ProfileMedicalCondition link = profileCondition();
        when(profileMedicalConditionRepository.findByProfileId(PROFILE_ID)).thenReturn(List.of(link));
        when(profileMedicalConditionMapper.toResponse(link)).thenReturn(
                new ProfileMedicalConditionResponse(link.getId(), PROFILE_ID, CONDITION_ID, "Diabetes", null));

        List<ProfileMedicalConditionResponse> responses = profileMedicalConditionService.listByProfile(PROFILE_ID, USER_ID);

        verify(profileService).getOwnedProfileEntity(PROFILE_ID, USER_ID);
        assertThat(responses).singleElement().satisfies(r -> assertThat(r.conditionName()).isEqualTo("Diabetes"));
    }

    @Test
    void addToProfileResolvesByConditionId() {
        MedicalCondition condition = condition("Asthma");
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicalConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(condition));
        when(profileMedicalConditionRepository.existsByProfileIdAndMedicalConditionId(PROFILE_ID, CONDITION_ID))
                .thenReturn(false);
        when(profileMedicalConditionRepository.save(any(ProfileMedicalCondition.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(profileMedicalConditionMapper.toResponse(any(ProfileMedicalCondition.class))).thenReturn(
                new ProfileMedicalConditionResponse(UUID.randomUUID(), PROFILE_ID, CONDITION_ID, "Asthma", null));

        ProfileMedicalConditionResponse response = profileMedicalConditionService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicalConditionRequest(CONDITION_ID, null, null));

        assertThat(response.medicalConditionId()).isEqualTo(CONDITION_ID);
        verify(catalogEntryCreator, never()).createMedicalCondition(any(), any());
    }

    @Test
    void addToProfileWithUnknownConditionIdThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicalConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileMedicalConditionService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicalConditionRequest(CONDITION_ID, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medical condition not found");
    }

    @Test
    void addToProfileWithNameUsesExistingCatalogEntry() {
        MedicalCondition condition = condition("Hypertension");
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicalConditionRepository.findByNameIgnoreCase("Hypertension")).thenReturn(Optional.of(condition));
        when(profileMedicalConditionRepository.existsByProfileIdAndMedicalConditionId(PROFILE_ID, CONDITION_ID))
                .thenReturn(false);
        when(profileMedicalConditionRepository.save(any(ProfileMedicalCondition.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(profileMedicalConditionMapper.toResponse(any(ProfileMedicalCondition.class))).thenReturn(
                new ProfileMedicalConditionResponse(UUID.randomUUID(), PROFILE_ID, CONDITION_ID, "Hypertension", null));

        ProfileMedicalConditionResponse response = profileMedicalConditionService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicalConditionRequest(null, "  Hypertension  ", "High BP"));

        assertThat(response.medicalConditionId()).isEqualTo(CONDITION_ID);
        verify(medicalConditionRepository).findByNameIgnoreCase("Hypertension");
    }

    @Test
    void addToProfileWithNameCreatesNewCatalogEntry() {
        MedicalCondition created = condition("Migraine");
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicalConditionRepository.findByNameIgnoreCase("Migraine")).thenReturn(Optional.empty());
        when(catalogEntryCreator.createMedicalCondition("Migraine", "Recurring")).thenReturn(created);
        when(profileMedicalConditionRepository.existsByProfileIdAndMedicalConditionId(PROFILE_ID, CONDITION_ID))
                .thenReturn(false);
        when(profileMedicalConditionRepository.save(any(ProfileMedicalCondition.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(profileMedicalConditionMapper.toResponse(any(ProfileMedicalCondition.class))).thenReturn(
                new ProfileMedicalConditionResponse(UUID.randomUUID(), PROFILE_ID, CONDITION_ID, "Migraine", null));

        ProfileMedicalConditionResponse response = profileMedicalConditionService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicalConditionRequest(null, "Migraine", "Recurring"));

        assertThat(response.conditionName()).isEqualTo("Migraine");
        verify(catalogEntryCreator).createMedicalCondition("Migraine", "Recurring");
    }

    @Test
    void addToProfileWhenCatalogSaveFailsFallsBackToConcurrentEntry() {
        MedicalCondition fallback = condition("Diabetes");
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicalConditionRepository.findByNameIgnoreCase("Diabetes"))
                .thenReturn(Optional.empty(), Optional.of(fallback));
        when(catalogEntryCreator.createMedicalCondition("Diabetes", null))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(profileMedicalConditionRepository.existsByProfileIdAndMedicalConditionId(PROFILE_ID, CONDITION_ID))
                .thenReturn(false);
        when(profileMedicalConditionRepository.save(any(ProfileMedicalCondition.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(profileMedicalConditionMapper.toResponse(any(ProfileMedicalCondition.class))).thenReturn(
                new ProfileMedicalConditionResponse(UUID.randomUUID(), PROFILE_ID, CONDITION_ID, "Diabetes", null));

        ProfileMedicalConditionResponse response = profileMedicalConditionService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicalConditionRequest(null, "Diabetes", null));

        assertThat(response.medicalConditionId()).isEqualTo(CONDITION_ID);
    }

    @Test
    void addToProfileWhenCatalogSaveFailsAndLookupMissingThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicalConditionRepository.findByNameIgnoreCase("Diabetes")).thenReturn(Optional.empty());
        when(catalogEntryCreator.createMedicalCondition("Diabetes", null))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(medicalConditionRepository.findByNameIgnoreCase("Diabetes")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileMedicalConditionService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicalConditionRequest(null, "Diabetes", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to create medical condition");
    }

    @Test
    void addToProfileWithDuplicateLinkThrows() {
        MedicalCondition condition = condition("Diabetes");
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicalConditionRepository.findByNameIgnoreCase("Diabetes")).thenReturn(Optional.of(condition));
        when(profileMedicalConditionRepository.existsByProfileIdAndMedicalConditionId(PROFILE_ID, CONDITION_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> profileMedicalConditionService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicalConditionRequest(null, "Diabetes", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already linked");
    }

    @Test
    void addToProfileWithBlankNameThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());

        assertThatThrownBy(() -> profileMedicalConditionService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicalConditionRequest(null, "   ", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Provide either medical condition id or name");
    }

    @Test
    void addToProfileWithNullNameThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());

        assertThatThrownBy(() -> profileMedicalConditionService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicalConditionRequest(null, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Provide either medical condition id or name");
    }

    @Test
    void addToProfileWithTooLongNameThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());

        assertThatThrownBy(() -> profileMedicalConditionService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileMedicalConditionRequest(null, "x".repeat(256), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not exceed 255");
    }

    @Test
    void removeFromProfileDeletesExistingLink() {
        ProfileMedicalCondition link = profileCondition();
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(profileMedicalConditionRepository.findByProfileIdAndMedicalConditionId(PROFILE_ID, CONDITION_ID))
                .thenReturn(Optional.of(link));

        profileMedicalConditionService.removeFromProfile(PROFILE_ID, USER_ID, CONDITION_ID);

        verify(profileMedicalConditionRepository).delete(link);
    }

    @Test
    void removeFromProfileWhenLinkMissingThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(profileMedicalConditionRepository.findByProfileIdAndMedicalConditionId(PROFILE_ID, CONDITION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileMedicalConditionService.removeFromProfile(PROFILE_ID, USER_ID, CONDITION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medical condition link not found");
    }

    @Test
    void listByProfileWithoutOwnershipCheckVerifiesExistence() {
        ProfileMedicalCondition link = profileCondition();
        when(profileService.getProfile(PROFILE_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(profileMedicalConditionRepository.findByProfileId(PROFILE_ID)).thenReturn(List.of(link));
        when(profileMedicalConditionMapper.toResponse(link)).thenReturn(
                new ProfileMedicalConditionResponse(link.getId(), PROFILE_ID, CONDITION_ID, "Diabetes", null));

        List<ProfileMedicalConditionResponse> responses = profileMedicalConditionService.listByProfile(PROFILE_ID);

        assertThat(responses).hasSize(1);
        verify(profileService).getProfile(PROFILE_ID);
    }
}
