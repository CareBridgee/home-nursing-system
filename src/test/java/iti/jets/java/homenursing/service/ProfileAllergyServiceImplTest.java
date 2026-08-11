package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.profile.ProfileAllergyRequest;
import iti.jets.java.homenursing.dto.profile.ProfileAllergyResponse;
import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ProfileAllergy;
import iti.jets.java.homenursing.entity.enums.AllergyType;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ProfileAllergyMapper;
import iti.jets.java.homenursing.repository.AllergyRepository;
import iti.jets.java.homenursing.repository.ProfileAllergyRepository;
import iti.jets.java.homenursing.service.impl.ProfileAllergyServiceImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ProfileAllergyServiceImplTest {

    @Mock
    private ProfileAllergyRepository profileAllergyRepository;
    @Mock
    private ProfileAllergyMapper profileAllergyMapper;
    @Mock
    private ProfileService profileService;
    @Mock
    private AllergyRepository allergyRepository;
    @Mock
    private CatalogEntryCreator catalogEntryCreator;

    @InjectMocks
    private ProfileAllergyServiceImpl profileAllergyService;

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ALLERGY_ID = UUID.randomUUID();

    private static Allergy allergy(String name, AllergyType type) {
        return Allergy.builder().id(ALLERGY_ID).name(name).type(type).build();
    }

    private static ProfileAllergy profileAllergy() {
        return ProfileAllergy.builder()
                .id(UUID.randomUUID())
                .profile(Profile.builder().id(PROFILE_ID).build())
                .allergy(allergy("Peanuts", AllergyType.FOOD))
                .build();
    }

    @Test
    void listByProfileWithOwnerVerifiesOwnershipAndMaps() {
        ProfileAllergy link = profileAllergy();
        when(profileAllergyRepository.findByProfileId(PROFILE_ID)).thenReturn(List.of(link));
        when(profileAllergyMapper.toResponse(link)).thenReturn(
                new ProfileAllergyResponse(link.getId(), PROFILE_ID, ALLERGY_ID, "Peanuts", AllergyType.FOOD, null));

        List<ProfileAllergyResponse> responses = profileAllergyService.listByProfile(PROFILE_ID, USER_ID);

        verify(profileService).getOwnedProfileEntity(PROFILE_ID, USER_ID);
        assertThat(responses).singleElement().satisfies(r -> assertThat(r.allergyName()).isEqualTo("Peanuts"));
    }

    @Test
    void addToProfileResolvesByAllergyId() {
        Allergy allergy = allergy("Penicillin", AllergyType.DRUG);
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(allergyRepository.findById(ALLERGY_ID)).thenReturn(Optional.of(allergy));
        when(profileAllergyRepository.existsByProfileIdAndAllergyId(PROFILE_ID, ALLERGY_ID)).thenReturn(false);
        when(profileAllergyRepository.save(any(ProfileAllergy.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileAllergyMapper.toResponse(any(ProfileAllergy.class))).thenReturn(
                new ProfileAllergyResponse(UUID.randomUUID(), PROFILE_ID, ALLERGY_ID, "Penicillin", AllergyType.DRUG, null));

        ProfileAllergyResponse response = profileAllergyService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileAllergyRequest(ALLERGY_ID, null, null));

        assertThat(response.allergyId()).isEqualTo(ALLERGY_ID);
        verify(catalogEntryCreator, never()).createAllergy(any(), any());
    }

    @Test
    void addToProfileWithUnknownAllergyIdThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(allergyRepository.findById(ALLERGY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileAllergyService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileAllergyRequest(ALLERGY_ID, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Allergy not found");
    }

    @Test
    void addToProfileWithNameUsesExistingCatalogEntry() {
        Allergy allergy = allergy("Peanuts", AllergyType.FOOD);
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(allergyRepository.findByNameIgnoreCase("Peanuts")).thenReturn(Optional.of(allergy));
        when(profileAllergyRepository.existsByProfileIdAndAllergyId(PROFILE_ID, ALLERGY_ID)).thenReturn(false);
        when(profileAllergyRepository.save(any(ProfileAllergy.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileAllergyMapper.toResponse(any(ProfileAllergy.class))).thenReturn(
                new ProfileAllergyResponse(UUID.randomUUID(), PROFILE_ID, ALLERGY_ID, "Peanuts", AllergyType.FOOD, null));

        ProfileAllergyResponse response = profileAllergyService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileAllergyRequest(null, "  Peanuts  ", AllergyType.FOOD));

        assertThat(response.allergyId()).isEqualTo(ALLERGY_ID);
        verify(allergyRepository).findByNameIgnoreCase("Peanuts");
    }

    @Test
    void addToProfileWithNameCreatesNewCatalogEntry() {
        Allergy created = allergy("Bee Venom", AllergyType.OTHER);
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(allergyRepository.findByNameIgnoreCase("Bee Venom")).thenReturn(Optional.empty());
        when(catalogEntryCreator.createAllergy("Bee Venom", AllergyType.OTHER)).thenReturn(created);
        when(profileAllergyRepository.existsByProfileIdAndAllergyId(PROFILE_ID, ALLERGY_ID)).thenReturn(false);
        when(profileAllergyRepository.save(any(ProfileAllergy.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileAllergyMapper.toResponse(any(ProfileAllergy.class))).thenReturn(
                new ProfileAllergyResponse(UUID.randomUUID(), PROFILE_ID, ALLERGY_ID, "Bee Venom", AllergyType.OTHER, null));

        ProfileAllergyResponse response = profileAllergyService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileAllergyRequest(null, "Bee Venom", AllergyType.OTHER));

        assertThat(response.allergyType()).isEqualTo(AllergyType.OTHER);
        verify(catalogEntryCreator).createAllergy("Bee Venom", AllergyType.OTHER);
    }

    @Test
    void addToProfileWhenCatalogSaveFailsFallsBackToConcurrentEntry() {
        Allergy fallback = allergy("Peanuts", AllergyType.FOOD);
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(allergyRepository.findByNameIgnoreCase("Peanuts")).thenReturn(Optional.empty(), Optional.of(fallback));
        when(catalogEntryCreator.createAllergy("Peanuts", AllergyType.FOOD))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(profileAllergyRepository.existsByProfileIdAndAllergyId(PROFILE_ID, ALLERGY_ID)).thenReturn(false);
        when(profileAllergyRepository.save(any(ProfileAllergy.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileAllergyMapper.toResponse(any(ProfileAllergy.class))).thenReturn(
                new ProfileAllergyResponse(UUID.randomUUID(), PROFILE_ID, ALLERGY_ID, "Peanuts", AllergyType.FOOD, null));

        ProfileAllergyResponse response = profileAllergyService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileAllergyRequest(null, "Peanuts", AllergyType.FOOD));

        assertThat(response.allergyId()).isEqualTo(ALLERGY_ID);
    }

    @Test
    void addToProfileWhenCatalogSaveFailsAndLookupMissingThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(allergyRepository.findByNameIgnoreCase("Peanuts")).thenReturn(Optional.empty());
        when(catalogEntryCreator.createAllergy("Peanuts", AllergyType.FOOD))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(allergyRepository.findByNameIgnoreCase("Peanuts")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileAllergyService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileAllergyRequest(null, "Peanuts", AllergyType.FOOD)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to create allergy");
    }

    @Test
    void addToProfileWithDuplicateLinkThrows() {
        Allergy allergy = allergy("Peanuts", AllergyType.FOOD);
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(allergyRepository.findByNameIgnoreCase("Peanuts")).thenReturn(Optional.of(allergy));
        when(profileAllergyRepository.existsByProfileIdAndAllergyId(PROFILE_ID, ALLERGY_ID)).thenReturn(true);

        assertThatThrownBy(() -> profileAllergyService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileAllergyRequest(null, "Peanuts", AllergyType.FOOD)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Allergy already linked");
    }

    @Test
    void addToProfileWithBlankNameThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());

        assertThatThrownBy(() -> profileAllergyService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileAllergyRequest(null, "   ", AllergyType.OTHER)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Provide either allergy id or name");
    }

    @Test
    void addToProfileWithNullNameThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());

        assertThatThrownBy(() -> profileAllergyService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileAllergyRequest(null, null, AllergyType.OTHER)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Provide either allergy id or name");
    }

    @Test
    void addToProfileWithTooLongNameThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());

        assertThatThrownBy(() -> profileAllergyService.addToProfile(
                PROFILE_ID, USER_ID, new ProfileAllergyRequest(null, "x".repeat(256), AllergyType.OTHER)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not exceed 255");
    }

    @Test
    void removeFromProfileDeletesExistingLink() {
        ProfileAllergy link = profileAllergy();
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(profileAllergyRepository.findByProfileIdAndAllergyId(PROFILE_ID, ALLERGY_ID)).thenReturn(Optional.of(link));

        profileAllergyService.removeFromProfile(PROFILE_ID, USER_ID, ALLERGY_ID);

        verify(profileAllergyRepository).delete(link);
    }

    @Test
    void removeFromProfileWhenLinkMissingThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(profileAllergyRepository.findByProfileIdAndAllergyId(PROFILE_ID, ALLERGY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileAllergyService.removeFromProfile(PROFILE_ID, USER_ID, ALLERGY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Allergy link not found");
    }

    @Test
    void listByProfileWithoutOwnershipCheckVerifiesExistence() {
        ProfileAllergy link = profileAllergy();
        when(profileService.getProfile(PROFILE_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(profileAllergyRepository.findByProfileId(PROFILE_ID)).thenReturn(List.of(link));
        when(profileAllergyMapper.toResponse(link)).thenReturn(
                new ProfileAllergyResponse(link.getId(), PROFILE_ID, ALLERGY_ID, "Peanuts", AllergyType.FOOD, null));

        List<ProfileAllergyResponse> responses = profileAllergyService.listByProfile(PROFILE_ID);

        assertThat(responses).hasSize(1);
        verify(profileService).getProfile(PROFILE_ID);
    }
}
