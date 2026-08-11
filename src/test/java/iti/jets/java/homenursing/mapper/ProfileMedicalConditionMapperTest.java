package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.profile.ProfileMedicalConditionResponse;
import iti.jets.java.homenursing.entity.MedicalCondition;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ProfileMedicalCondition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ProfileMedicalConditionMapperTest {

    private final ProfileMedicalConditionMapper mapper = Mappers.getMapper(ProfileMedicalConditionMapper.class);

    @Test
    void toResponse_mapsAllFields() {
        UUID profileId = UUID.randomUUID();
        UUID conditionId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 2, 2, 2, 2);
        Profile profile = Profile.builder().id(profileId).build();
        MedicalCondition condition = MedicalCondition.builder().id(conditionId).name("Hypertension").build();
        ProfileMedicalCondition entity = ProfileMedicalCondition.builder()
                .id(id)
                .profile(profile)
                .medicalCondition(condition)
                .createdAt(createdAt)
                .build();

        ProfileMedicalConditionResponse response = mapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.profileId()).isEqualTo(profileId);
        assertThat(response.medicalConditionId()).isEqualTo(conditionId);
        assertThat(response.conditionName()).isEqualTo("Hypertension");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toResponse_nullProfile_yieldsNullProfileId() {
        ProfileMedicalCondition entity = ProfileMedicalCondition.builder().profile(null).build();

        assertThat(mapper.toResponse(entity).profileId()).isNull();
    }

    @Test
    void toResponse_nullCondition_yieldsNullConditionFields() {
        ProfileMedicalCondition entity = ProfileMedicalCondition.builder().medicalCondition(null).build();

        ProfileMedicalConditionResponse response = mapper.toResponse(entity);

        assertThat(response.medicalConditionId()).isNull();
        assertThat(response.conditionName()).isNull();
    }

    @Test
    void toResponse_nullEntity_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
