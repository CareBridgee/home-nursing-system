package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.profile.MedicalHistoryRequest;
import iti.jets.java.homenursing.dto.profile.MedicalHistoryResponse;
import iti.jets.java.homenursing.entity.MedicalHistory;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.enums.MedicalHistoryType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class MedicalHistoryMapperTest {

    private final MedicalHistoryMapper mapper = Mappers.getMapper(MedicalHistoryMapper.class);

    @Test
    void toEntity_mapsAllRequestFields() {
        MedicalHistoryRequest request = MedicalHistoryRequest.builder()
                .type(MedicalHistoryType.SURGERY)
                .description("Appendectomy in 2019")
                .build();

        MedicalHistory entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getType()).isEqualTo(MedicalHistoryType.SURGERY);
        assertThat(entity.getDescription()).isEqualTo("Appendectomy in 2019");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getProfile()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_nullRequest_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_mapsAllFieldsIncludingProfileId() {
        UUID profileId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 20, 16, 0);
        LocalDateTime updatedAt = createdAt.plusHours(3);
        Profile profile = Profile.builder().id(profileId).build();
        MedicalHistory history = MedicalHistory.builder()
                .id(id)
                .profile(profile)
                .type(MedicalHistoryType.HOSPITALIZATION)
                .description("Stayed 3 days")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        MedicalHistoryResponse response = mapper.toResponse(history);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getProfileId()).isEqualTo(profileId);
        assertThat(response.getType()).isEqualTo(MedicalHistoryType.HOSPITALIZATION);
        assertThat(response.getDescription()).isEqualTo("Stayed 3 days");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toResponse_nullProfile_yieldsNullProfileId() {
        MedicalHistory history = MedicalHistory.builder().profile(null).build();

        assertThat(mapper.toResponse(history).getProfileId()).isNull();
    }

    @Test
    void toResponse_nullHistory_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
