package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.user.UserRequest;
import iti.jets.java.homenursing.dto.user.UserResponse;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.Gender;
import iti.jets.java.homenursing.util.ProfileCompletionChecker;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Mock
    private ProfileCompletionChecker profileCompletionChecker;

    // The generated UserMapperImpl wires ProfileCompletionChecker through a private
    // @Autowired field, so Mappers.getMapper(UserMapper.class) would leave it null
    // and toResponse would NPE. Mockito @InjectMocks is the only non-hand-rolled
    // seam to wire it without a Spring context.
    @InjectMocks
    private UserMapperImpl mapper;

    @Test
    void toEntity_mapsAllRequestFields() {
        UserRequest request = UserRequest.builder()
                .phoneNumber("+201112223334")
                .firstName("Nour")
                .lastName("Adel")
                .dateOfBirth("1993-06-15")
                .gender(Gender.FEMALE)
                .profileImageUrl("https://img.example/nour.png")
                .build();

        User entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getPhoneNumber()).isEqualTo("+201112223334");
        assertThat(entity.getFirstName()).isEqualTo("Nour");
        assertThat(entity.getLastName()).isEqualTo("Adel");
        assertThat(entity.getDateOfBirth()).isEqualTo(LocalDate.of(1993, 6, 15));
        assertThat(entity.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(entity.getProfileImageUrl()).isEqualTo("https://img.example/nour.png");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getEmail()).isNull();
    }

    @Test
    void toEntity_nullDateOfBirth_isSkipped() {
        UserRequest request = UserRequest.builder()
                .firstName("Nour")
                .lastName("Adel")
                .dateOfBirth(null)
                .build();

        User entity = mapper.toEntity(request);

        assertThat(entity.getDateOfBirth()).isNull();
        assertThat(entity.getFirstName()).isEqualTo("Nour");
    }

    @Test
    void toEntity_nullRequest_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_mapsAllFieldsWithPrimaryProfile() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime updatedAt = createdAt.plusDays(1);
        LocalDateTime lastLoginAt = createdAt.plusHours(5);
        User user = User.builder()
                .id(userId)
                .phoneNumber("+201112223334")
                .email("nour@example.com")
                .firstName("Nour")
                .lastName("Adel")
                .dateOfBirth(LocalDate.of(1993, 6, 15))
                .gender(Gender.FEMALE)
                .profileImageUrl("https://img.example/nour.png")
                .isDeleted(false)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .lastLoginAt(lastLoginAt)
                .profiles(List.of(
                        Profile.builder().id(UUID.randomUUID()).isPrimary(false).build(),
                        Profile.builder().id(profileId).isPrimary(true).build()
                ))
                .build();
        when(profileCompletionChecker.isUserProfileCompleted(user)).thenReturn(true);

        UserResponse response = mapper.toResponse(user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getPhoneNumber()).isEqualTo("+201112223334");
        assertThat(response.getEmail()).isEqualTo("nour@example.com");
        assertThat(response.getFirstName()).isEqualTo("Nour");
        assertThat(response.getLastName()).isEqualTo("Adel");
        assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1993, 6, 15));
        assertThat(response.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(response.getProfileImageUrl()).isEqualTo("https://img.example/nour.png");
        assertThat(response.getIsDeleted()).isFalse();
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(response.getLastLoginAt()).isEqualTo(lastLoginAt);
        assertThat(response.getDefaultProfileId()).isEqualTo(profileId);
        assertThat(response.getProfileCompleted()).isTrue();
    }

    @Test
    void toResponse_incompleteProfile_isFalse() {
        User user = userWithId();
        when(profileCompletionChecker.isUserProfileCompleted(user)).thenReturn(false);

        assertThat(mapper.toResponse(user).getProfileCompleted()).isFalse();
    }

    @Test
    void toResponse_nullProfiles_yieldsNullDefaultProfileId() {
        User user = userWithId();
        user.setProfiles(null);

        assertThat(mapper.toResponse(user).getDefaultProfileId()).isNull();
    }

    @Test
    void toResponse_noPrimaryProfile_yieldsNullDefaultProfileId() {
        User user = userWithId();
        user.setProfiles(List.of(
                Profile.builder().id(UUID.randomUUID()).isPrimary(false).build(),
                Profile.builder().id(UUID.randomUUID()).isPrimary(false).build()
        ));

        assertThat(mapper.toResponse(user).getDefaultProfileId()).isNull();
    }

    @Test
    void toResponse_emptyProfiles_yieldsNullDefaultProfileId() {
        User user = userWithId();
        user.setProfiles(List.of());

        assertThat(mapper.toResponse(user).getDefaultProfileId()).isNull();
    }

    @Test
    void toResponse_nullUser_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    private User userWithId() {
        return User.builder()
                .id(UUID.randomUUID())
                .firstName("Nour")
                .lastName("Adel")
                .dateOfBirth(LocalDate.of(1993, 6, 15))
                .gender(Gender.FEMALE)
                .build();
    }
}
