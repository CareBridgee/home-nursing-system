package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.user.UserResponse;
import iti.jets.java.homenursing.dto.user.UserUpdateRequest;
import iti.jets.java.homenursing.entity.Address;
import iti.jets.java.homenursing.entity.EmergencyContact;
import iti.jets.java.homenursing.entity.MedicalHistory;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ReviewRating;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.Gender;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.UserMapper;
import iti.jets.java.homenursing.repository.AddressRepository;
import iti.jets.java.homenursing.repository.EmergencyContactRepository;
import iti.jets.java.homenursing.repository.MedicalHistoryRepository;
import iti.jets.java.homenursing.repository.ProfileRepository;
import iti.jets.java.homenursing.repository.ReviewRatingRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.impl.UserServiceImpl;
import iti.jets.java.homenursing.util.NurseRatingUpdater;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private MedicalHistoryRepository medicalHistoryRepository;
    @Mock
    private EmergencyContactRepository emergencyContactRepository;
    @Mock
    private ReviewRatingRepository reviewRatingRepository;
    @Mock
    private NurseRatingUpdater nurseRatingUpdater;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private UserServiceImpl userService;

    private static final UUID USER_ID = UUID.randomUUID();

    private static User user(boolean deleted) {
        return User.builder()
                .id(USER_ID)
                .firstName("First")
                .lastName("Last")
                .profileImageUrl("old-image")
                .isDeleted(deleted)
                .build();
    }

    private static UserResponse response() {
        return UserResponse.builder().id(USER_ID).firstName("First").lastName("Last").build();
    }

    @Test
    void getCurrentUserReturnsMappedActiveUser() {
        User user = user(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response());

        UserResponse result = userService.getCurrentUser(USER_ID);

        assertThat(result.getId()).isEqualTo(USER_ID);
    }

    @Test
    void getByIdReturnsMappedActiveUser() {
        User user = user(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response());

        UserResponse result = userService.getById(USER_ID);

        assertThat(result.getId()).isEqualTo(USER_ID);
    }

    @Test
    void getCurrentUserWhenDeletedThrows() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(true)));

        assertThatThrownBy(() -> userService.getCurrentUser(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getCurrentUserWhenMissingThrows() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateCurrentUserAppliesAllFieldsUploadsImageAndSyncsPrimaryProfile() {
        User user = user(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        Profile primary = Profile.builder().id(UUID.randomUUID()).build();
        when(profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(USER_ID)).thenReturn(Optional.of(primary));
        UserUpdateRequest request = UserUpdateRequest.builder()
                .firstName("NewFirst")
                .lastName("NewLast")
                .email("new@example.com")
                .dateOfBirth(LocalDate.of(1992, 3, 4))
                .gender(Gender.FEMALE)
                .profileImageUrl("direct-url")
                .profileImage(new MockMultipartFile("f", "a.jpg", "image/jpeg", new byte[]{1}))
                .build();
        when(cloudinaryService.upload(any(MultipartFile.class))).thenReturn("cloud-url");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response());

        userService.updateCurrentUser(USER_ID, request);

        assertThat(user.getFirstName()).isEqualTo("NewFirst");
        assertThat(user.getLastName()).isEqualTo("NewLast");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getDateOfBirth()).isEqualTo(LocalDate.of(1992, 3, 4));
        assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(user.getProfileImageUrl()).isEqualTo("cloud-url");
        assertThat(primary.getProfileImageUrl()).isEqualTo("cloud-url");
        verify(profileRepository).save(primary);
        verify(userRepository).save(user);
    }

    @Test
    void updateCurrentUserWithNullFieldsAndEmptyImageLeavesUserUntouched() {
        User user = user(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(USER_ID)).thenReturn(Optional.empty());
        UserUpdateRequest request = UserUpdateRequest.builder()
                .profileImage(new MockMultipartFile("f", new byte[0]))
                .build();
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response());

        userService.updateCurrentUser(USER_ID, request);

        assertThat(user.getFirstName()).isEqualTo("First");
        assertThat(user.getProfileImageUrl()).isEqualTo("old-image");
        verify(cloudinaryService, never()).upload(any(MultipartFile.class));
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void updateCurrentUserWithoutPrimaryProfileSkipsSync() {
        User user = user(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(USER_ID)).thenReturn(Optional.empty());
        UserUpdateRequest request = UserUpdateRequest.builder().firstName("Only").build();
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response());

        userService.updateCurrentUser(USER_ID, request);

        assertThat(user.getFirstName()).isEqualTo("Only");
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void deleteCurrentUserSoftDeletesUserAndCascadesProfileData() {
        User user = user(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UUID profileId = UUID.randomUUID();
        Profile profile = Profile.builder().id(profileId).build();
        when(profileRepository.findByUserIdAndIsDeletedFalse(USER_ID)).thenReturn(List.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);
        Address address = Address.builder().id(UUID.randomUUID()).city("Cairo").build();
        when(addressRepository.findByProfileId(profileId)).thenReturn(Optional.of(address));
        MedicalHistory history = MedicalHistory.builder().id(UUID.randomUUID()).build();
        when(medicalHistoryRepository.findByProfileIdOrderByCreatedAtDesc(profileId)).thenReturn(List.of(history));
        EmergencyContact contact = EmergencyContact.builder().id(UUID.randomUUID()).build();
        when(emergencyContactRepository.findByProfileId(profileId)).thenReturn(List.of(contact));
        ReviewRating review = ReviewRating.builder()
                .id(UUID.randomUUID())
                .nurse(Nurse.builder().id(UUID.randomUUID()).build())
                .rating(4)
                .build();
        when(reviewRatingRepository.findByProfileId(profileId)).thenReturn(List.of(review));

        userService.deleteCurrentUser(USER_ID);

        assertThat(user.getIsDeleted()).isTrue();
        assertThat(profile.getIsDeleted()).isTrue();
        verify(userRepository).save(user);
        verify(profileRepository).save(profile);
        verify(addressRepository).delete(address);
        verify(medicalHistoryRepository).delete(history);
        verify(emergencyContactRepository).delete(contact);
        verify(reviewRatingRepository).delete(review);
        verify(nurseRatingUpdater).onReviewDeleted(review.getNurse(), review.getRating());
    }

    @Test
    void deleteCurrentUserWithoutAssociationsSkipsDeletes() {
        User user = user(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UUID profileId = UUID.randomUUID();
        Profile profile = Profile.builder().id(profileId).build();
        when(profileRepository.findByUserIdAndIsDeletedFalse(USER_ID)).thenReturn(List.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);
        when(addressRepository.findByProfileId(profileId)).thenReturn(Optional.empty());

        userService.deleteCurrentUser(USER_ID);

        verify(addressRepository, never()).delete(any());
        verify(medicalHistoryRepository, never()).delete(any());
        verify(emergencyContactRepository, never()).delete(any());
        verify(reviewRatingRepository, never()).delete(any());
    }

    @Test
    void listUsersSanitizesPageableAndMaps() {
        User user = user(false);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toResponse(user)).thenReturn(response());

        Page<UserResponse> page = userService.listUsers(Pageable.unpaged());

        assertThat(page.getContent()).singleElement().satisfies(r -> assertThat(r.getId()).isEqualTo(USER_ID));
    }
}
