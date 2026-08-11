package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.profile.ProfileMedicationResponse;
import iti.jets.java.homenursing.entity.Medication;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ProfileMedication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ProfileMedicationMapperTest {

    private final ProfileMedicationMapper mapper = Mappers.getMapper(ProfileMedicationMapper.class);

    @Test
    void toResponse_mapsAllFields() {
        UUID profileId = UUID.randomUUID();
        UUID medicationId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 3, 3, 3);
        Profile profile = Profile.builder().id(profileId).build();
        Medication medication = Medication.builder().id(medicationId).name("Paracetamol").build();
        ProfileMedication entity = ProfileMedication.builder()
                .id(id)
                .profile(profile)
                .medication(medication)
                .createdAt(createdAt)
                .build();

        ProfileMedicationResponse response = mapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.profileId()).isEqualTo(profileId);
        assertThat(response.medicationId()).isEqualTo(medicationId);
        assertThat(response.medicationName()).isEqualTo("Paracetamol");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toResponse_nullProfile_yieldsNullProfileId() {
        ProfileMedication entity = ProfileMedication.builder().profile(null).build();

        assertThat(mapper.toResponse(entity).profileId()).isNull();
    }

    @Test
    void toResponse_nullMedication_yieldsNullMedicationFields() {
        ProfileMedication entity = ProfileMedication.builder().medication(null).build();

        ProfileMedicationResponse response = mapper.toResponse(entity);

        assertThat(response.medicationId()).isNull();
        assertThat(response.medicationName()).isNull();
    }

    @Test
    void toResponse_nullEntity_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
