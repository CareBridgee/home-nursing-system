package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.profile.ProfileRequest;
import iti.jets.java.homenursing.dto.profile.ProfileResponse;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.Gender;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ProfileMapper;
import iti.jets.java.homenursing.repository.ProfileRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ProfileMapper profileMapper;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();

    private static User user(boolean withProfiles) {
        User.UserBuilder builder = User.builder()
                .id(USER_ID)
                .firstName("First")
                .lastName("Last")
                .dateOfBirth(LocalDate.of(1990, 5, 10))
                .gender(Gender.MALE)
                .profileImageUrl("user-image");
        if (withProfiles) {
            return builder.profiles(new ArrayList<>()).build();
        }
        return builder.profiles(null).build();
    }

    private static Profile profile(UUID id, UUID userId, Boolean isPrimary, Boolean isDeleted) {
        return Profile.builder()
                .id(id)
                .user(User.builder().id(userId).build())
                .firstName("First")
                .lastName("Last")
                .isPrimary(isPrimary)
                .isDeleted(isDeleted)
                .profileImageUrl("image-url")
                .build();
    }

    @Test
    void createDefaultProfileCopiesUserFieldsAndAddsToProfileList() {
        User user = user(true);
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = profileService.createDefaultProfile(user);

        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getIsPrimary()).isTrue();
        assertThat(result.getIsDeleted()).isFalse();
        assertThat(result.getFirstName()).isEqualTo("First");
        assertThat(result.getLastName()).isEqualTo("Last");
        assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 10));
        assertThat(result.getGender()).isEqualTo(Gender.MALE);
        assertThat(result.getProfileImageUrl()).isEqualTo("user-image");
        assertThat(user.getProfiles()).contains(result);
    }

    @Test
    void createDefaultProfileSkipsListAddWhenProfilesNull() {
        User user = user(false);
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = profileService.createDefaultProfile(user);

        assertThat(result.getUser()).isSameAs(user);
        assertThat(user.getProfiles()).isNull();
    }

    @Test
    void getDefaultProfileReturnsMappedResponse() {
        Profile profile = profile(PROFILE_ID, USER_ID, true, false);
        when(profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(USER_ID)).thenReturn(Optional.of(profile));
        when(profileMapper.toResponse(profile)).thenReturn(ProfileResponse.builder().id(PROFILE_ID).build());

        ProfileResponse response = profileService.getDefaultProfile(USER_ID);

        assertThat(response.getId()).isEqualTo(PROFILE_ID);
    }

    @Test
    void getDefaultProfileWhenMissingThrows() {
        when(profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getDefaultProfile(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Default profile not found");
    }

    @Test
    void listProfilesReturnsMappedList() {
        Profile first = profile(UUID.randomUUID(), USER_ID, true, false);
        Profile second = profile(UUID.randomUUID(), USER_ID, false, false);
        when(profileRepository.findByUserIdAndIsDeletedFalse(USER_ID)).thenReturn(List.of(first, second));
        when(profileMapper.toResponse(first)).thenReturn(ProfileResponse.builder().id(first.getId()).build());
        when(profileMapper.toResponse(second)).thenReturn(ProfileResponse.builder().id(second.getId()).build());

        List<ProfileResponse> responses = profileService.listProfiles(USER_ID);

        assertThat(responses).extracting(ProfileResponse::getId).containsExactly(first.getId(), second.getId());
    }

    @Test
    void getOwnedProfileReturnsMappedResponse() {
        Profile profile = profile(PROFILE_ID, USER_ID, false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(profileMapper.toResponse(profile)).thenReturn(ProfileResponse.builder().id(PROFILE_ID).build());

        ProfileResponse response = profileService.getOwnedProfile(PROFILE_ID, USER_ID);

        assertThat(response.getId()).isEqualTo(PROFILE_ID);
    }

    @Test
    void getOwnedProfileWhenNotOwnerThrows() {
        Profile profile = profile(PROFILE_ID, UUID.randomUUID(), false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> profileService.getOwnedProfile(PROFILE_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }

    @Test
    void getOwnedProfileEntityReturnsEntity() {
        Profile profile = profile(PROFILE_ID, USER_ID, false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        Profile result = profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID);

        assertThat(result).isSameAs(profile);
    }

    @Test
    void getAccessibleProfileWhenOwnerReturnsResponse() {
        Profile profile = profile(PROFILE_ID, USER_ID, false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(profileMapper.toResponse(profile)).thenReturn(ProfileResponse.builder().id(PROFILE_ID).build());

        ProfileResponse response = profileService.getAccessibleProfile(PROFILE_ID, USER_ID);

        assertThat(response.getId()).isEqualTo(PROFILE_ID);
        verify(serviceRequestRepository, never()).existsByProfile_IdAndNurse_User_IdAndIsDeletedFalseAndStatusIn(
                any(), any(), anySet());
    }

    @Test
    void getAccessibleProfileWhenAssignedNurseReturnsResponse() {
        UUID nurseUserId = UUID.randomUUID();
        Profile profile = profile(PROFILE_ID, USER_ID, false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(serviceRequestRepository.existsByProfile_IdAndNurse_User_IdAndIsDeletedFalseAndStatusIn(
                eq(PROFILE_ID), eq(nurseUserId), anySet())).thenReturn(true);
        when(profileMapper.toResponse(profile)).thenReturn(ProfileResponse.builder().id(PROFILE_ID).build());

        ProfileResponse response = profileService.getAccessibleProfile(PROFILE_ID, nurseUserId);

        assertThat(response.getId()).isEqualTo(PROFILE_ID);
    }

    @Test
    void getAccessibleProfileWhenUnrelatedUserThrows() {
        UUID stranger = UUID.randomUUID();
        Profile profile = profile(PROFILE_ID, USER_ID, false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(serviceRequestRepository.existsByProfile_IdAndNurse_User_IdAndIsDeletedFalseAndStatusIn(
                eq(PROFILE_ID), eq(stranger), anySet())).thenReturn(false);

        assertThatThrownBy(() -> profileService.getAccessibleProfile(PROFILE_ID, stranger))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }

    @Test
    void getAccessibleProfileEntityReturnsEntityForOwner() {
        Profile profile = profile(PROFILE_ID, USER_ID, false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        Profile result = profileService.getAccessibleProfileEntity(PROFILE_ID, USER_ID);

        assertThat(result).isSameAs(profile);
    }

    @Test
    void createFamilyProfileBuildsNonPrimaryProfile() {
        Profile primary = profile(PROFILE_ID, USER_ID, true, false);
        when(profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(USER_ID)).thenReturn(Optional.of(primary));
        ProfileRequest request = ProfileRequest.builder()
                .firstName("Child")
                .lastName("Last")
                .relationship("Son")
                .build();
        Profile mapped = Profile.builder().firstName("Child").lastName("Last").build();
        when(profileMapper.toEntity(request)).thenReturn(mapped);
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileMapper.toResponse(any(Profile.class))).thenReturn(ProfileResponse.builder().id(PROFILE_ID).build());

        ProfileResponse response = profileService.createFamilyProfile(USER_ID, request);

        assertThat(mapped.getUser()).isSameAs(primary.getUser());
        assertThat(mapped.getIsPrimary()).isFalse();
        assertThat(mapped.getIsDeleted()).isFalse();
        assertThat(response.getId()).isEqualTo(PROFILE_ID);
        verify(profileRepository).save(mapped);
    }

    @Test
    void createFamilyProfileAppliesImageUrl() {
        Profile primary = profile(PROFILE_ID, USER_ID, true, false);
        when(profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(USER_ID)).thenReturn(Optional.of(primary));
        ProfileRequest request = ProfileRequest.builder().profileImageUrl("family-image").build();
        Profile mapped = Profile.builder().build();
        when(profileMapper.toEntity(request)).thenReturn(mapped);
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileMapper.toResponse(any(Profile.class))).thenReturn(ProfileResponse.builder().build());

        profileService.createFamilyProfile(USER_ID, request);

        assertThat(mapped.getProfileImageUrl()).isEqualTo("family-image");
    }

    @Test
    void createFamilyProfileWithoutPrimaryThrows() {
        when(profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.createFamilyProfile(USER_ID, ProfileRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Default profile not found");
    }

    @Test
    void updateProfileAppliesAllProvidedFieldsAndUploadsImage() {
        Profile profile = profile(PROFILE_ID, USER_ID, false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        ProfileRequest request = ProfileRequest.builder()
                .relationship("Spouse")
                .firstName("New")
                .lastName("Name")
                .dateOfBirth(LocalDate.of(1985, 1, 1))
                .gender(Gender.FEMALE)
                .bloodType("O+")
                .height(new BigDecimal("170.0"))
                .weight(new BigDecimal("70.0"))
                .mobilityStatus("INDEPENDENT")
                .mobilityNotes("Walks unaided")
                .previousSurgeries("Appendectomy")
                .previousHospitalizations("None")
                .profileImageUrl("direct-url")
                .profileImage(new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2}))
                .build();
        when(cloudinaryService.upload(any(MultipartFile.class))).thenReturn("cloud-url");
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileMapper.toResponse(any(Profile.class))).thenReturn(ProfileResponse.builder().build());

        profileService.updateProfile(PROFILE_ID, USER_ID, request);

        assertThat(profile.getRelationship()).isEqualTo("Spouse");
        assertThat(profile.getFirstName()).isEqualTo("New");
        assertThat(profile.getLastName()).isEqualTo("Name");
        assertThat(profile.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 1, 1));
        assertThat(profile.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(profile.getBloodType()).isEqualTo("O+");
        assertThat(profile.getHeight()).isEqualByComparingTo("170.0");
        assertThat(profile.getWeight()).isEqualByComparingTo("70.0");
        assertThat(profile.getMobilityStatus()).isEqualTo("INDEPENDENT");
        assertThat(profile.getMobilityNotes()).isEqualTo("Walks unaided");
        assertThat(profile.getPreviousSurgeries()).isEqualTo("Appendectomy");
        assertThat(profile.getPreviousHospitalizations()).isEqualTo("None");
        assertThat(profile.getProfileImageUrl()).isEqualTo("cloud-url");
        verify(profileRepository).save(profile);
    }

    @Test
    void updateProfileWithNullRequestFieldsLeavesEntityUntouched() {
        Profile profile = profile(PROFILE_ID, USER_ID, false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        ProfileRequest request = ProfileRequest.builder().build();
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileMapper.toResponse(any(Profile.class))).thenReturn(ProfileResponse.builder().build());

        profileService.updateProfile(PROFILE_ID, USER_ID, request);

        assertThat(profile.getFirstName()).isEqualTo("First");
        assertThat(profile.getProfileImageUrl()).isEqualTo("image-url");
        verify(cloudinaryService, never()).upload(any(MultipartFile.class));
    }

    @Test
    void updateProfileSkipsUploadForEmptyImage() {
        Profile profile = profile(PROFILE_ID, USER_ID, false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        ProfileRequest request = ProfileRequest.builder()
                .profileImage(new MockMultipartFile("file", new byte[0]))
                .build();
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileMapper.toResponse(any(Profile.class))).thenReturn(ProfileResponse.builder().build());

        profileService.updateProfile(PROFILE_ID, USER_ID, request);

        assertThat(profile.getProfileImageUrl()).isEqualTo("image-url");
        verify(cloudinaryService, never()).upload(any(MultipartFile.class));
    }

    @Test
    void deleteFamilyProfileWithPrimaryThrows() {
        Profile primary = profile(PROFILE_ID, USER_ID, true, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(primary));

        assertThatThrownBy(() -> profileService.deleteFamilyProfile(PROFILE_ID, USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete the primary profile");
    }

    @Test
    void deleteFamilyProfileSoftDeletesNonPrimary() {
        Profile profile = profile(PROFILE_ID, USER_ID, false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        profileService.deleteFamilyProfile(PROFILE_ID, USER_ID);

        assertThat(profile.getIsDeleted()).isTrue();
        verify(profileRepository).save(profile);
    }

    @Test
    void deleteFamilyProfileByAnotherUserThrows() {
        Profile profile = profile(PROFILE_ID, UUID.randomUUID(), false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> profileService.deleteFamilyProfile(PROFILE_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }

    @Test
    void getOwnedProfileWhenDeletedThrows() {
        Profile deleted = profile(PROFILE_ID, USER_ID, false, true);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> profileService.getOwnedProfile(PROFILE_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }

    @Test
    void getOwnedProfileWhenMissingThrows() {
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getOwnedProfile(PROFILE_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }

    @Test
    void getProfileReturnsEntity() {
        Profile profile = profile(PROFILE_ID, USER_ID, false, false);
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        Profile result = profileService.getProfile(PROFILE_ID);

        assertThat(result).isSameAs(profile);
    }

    @Test
    void getProfileWhenMissingThrows() {
        when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(PROFILE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }
}
