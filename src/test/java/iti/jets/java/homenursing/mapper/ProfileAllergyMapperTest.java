package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.profile.ProfileAllergyResponse;
import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ProfileAllergy;
import iti.jets.java.homenursing.entity.enums.AllergyType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ProfileAllergyMapperTest {

    private final ProfileAllergyMapper mapper = Mappers.getMapper(ProfileAllergyMapper.class);

    @Test
    void toResponse_mapsAllFields() {
        UUID profileId = UUID.randomUUID();
        UUID allergyId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        Profile profile = Profile.builder().id(profileId).build();
        Allergy allergy = Allergy.builder().id(allergyId).name("Latex").type(AllergyType.OTHER).build();
        ProfileAllergy entity = ProfileAllergy.builder()
                .id(id)
                .profile(profile)
                .allergy(allergy)
                .createdAt(createdAt)
                .build();

        ProfileAllergyResponse response = mapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.profileId()).isEqualTo(profileId);
        assertThat(response.allergyId()).isEqualTo(allergyId);
        assertThat(response.allergyName()).isEqualTo("Latex");
        assertThat(response.allergyType()).isEqualTo(AllergyType.OTHER);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toResponse_nullProfile_yieldsNullProfileId() {
        ProfileAllergy entity = ProfileAllergy.builder().profile(null).build();

        assertThat(mapper.toResponse(entity).profileId()).isNull();
    }

    @Test
    void toResponse_nullAllergy_yieldsNullAllergyFields() {
        ProfileAllergy entity = ProfileAllergy.builder().allergy(null).build();

        ProfileAllergyResponse response = mapper.toResponse(entity);

        assertThat(response.allergyId()).isNull();
        assertThat(response.allergyName()).isNull();
        assertThat(response.allergyType()).isNull();
    }

    @Test
    void toResponse_nullEntity_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
