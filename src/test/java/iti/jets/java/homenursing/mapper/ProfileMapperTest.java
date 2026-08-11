package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.profile.ProfileRequest;
import iti.jets.java.homenursing.dto.profile.ProfileResponse;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.Gender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ProfileMapperTest {

    private final ProfileMapper mapper = Mappers.getMapper(ProfileMapper.class);

    @Test
    void toEntity_mapsAllRequestFields() {
        ProfileRequest request = ProfileRequest.builder()
                .relationship("Self")
                .firstName("Ahmed")
                .lastName("Kamel")
                .dateOfBirth(LocalDate.of(1985, 4, 12))
                .gender(Gender.MALE)
                .bloodType("O+")
                .height(new BigDecimal("175.00"))
                .weight(new BigDecimal("80.00"))
                .mobilityStatus("INDEPENDENT")
                .mobilityNotes("Walks unaided")
                .previousSurgeries("None")
                .previousHospitalizations("2020")
                .profileImageUrl("https://img.example/me.png")
                .build();

        Profile entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getRelationship()).isEqualTo("Self");
        assertThat(entity.getFirstName()).isEqualTo("Ahmed");
        assertThat(entity.getLastName()).isEqualTo("Kamel");
        assertThat(entity.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 4, 12));
        assertThat(entity.getGender()).isEqualTo(Gender.MALE);
        assertThat(entity.getBloodType()).isEqualTo("O+");
        assertThat(entity.getHeight()).isEqualByComparingTo("175.00");
        assertThat(entity.getWeight()).isEqualByComparingTo("80.00");
        assertThat(entity.getMobilityStatus()).isEqualTo("INDEPENDENT");
        assertThat(entity.getMobilityNotes()).isEqualTo("Walks unaided");
        assertThat(entity.getPreviousSurgeries()).isEqualTo("None");
        assertThat(entity.getPreviousHospitalizations()).isEqualTo("2020");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getUser()).isNull();
        assertThat(entity.getIsPrimary()).isFalse();
        assertThat(entity.getProfileImageUrl()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_nullRequest_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_mapsAllFieldsIncludingUserId() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 12, 0);
        LocalDateTime updatedAt = createdAt.plusDays(2);
        User user = User.builder().id(userId).build();
        Profile profile = Profile.builder()
                .id(id)
                .user(user)
                .relationship("Self")
                .firstName("Mona")
                .lastName("Said")
                .dateOfBirth(LocalDate.of(1992, 9, 30))
                .gender(Gender.FEMALE)
                .bloodType("A+")
                .height(new BigDecimal("162.00"))
                .weight(new BigDecimal("55.00"))
                .mobilityStatus("PARTIAL_ASSISTANCE")
                .mobilityNotes("Needs help standing")
                .previousSurgeries("Knee surgery")
                .previousHospitalizations("None")
                .profileImageUrl("https://img.example/mona.png")
                .isPrimary(true)
                .isDeleted(false)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        ProfileResponse response = mapper.toResponse(profile);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRelationship()).isEqualTo("Self");
        assertThat(response.getFirstName()).isEqualTo("Mona");
        assertThat(response.getLastName()).isEqualTo("Said");
        assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1992, 9, 30));
        assertThat(response.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(response.getBloodType()).isEqualTo("A+");
        assertThat(response.getHeight()).isEqualByComparingTo("162.00");
        assertThat(response.getWeight()).isEqualByComparingTo("55.00");
        assertThat(response.getMobilityStatus()).isEqualTo("PARTIAL_ASSISTANCE");
        assertThat(response.getMobilityNotes()).isEqualTo("Needs help standing");
        assertThat(response.getPreviousSurgeries()).isEqualTo("Knee surgery");
        assertThat(response.getPreviousHospitalizations()).isEqualTo("None");
        assertThat(response.getProfileImageUrl()).isEqualTo("https://img.example/mona.png");
        assertThat(response.getIsPrimary()).isTrue();
        assertThat(response.getIsDeleted()).isFalse();
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toResponse_nullUser_yieldsNullUserId() {
        Profile profile = Profile.builder().user(null).build();

        assertThat(mapper.toResponse(profile).getUserId()).isNull();
    }

    @Test
    void toResponse_nullProfile_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
