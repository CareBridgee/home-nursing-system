package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.catalog.MedicationRequest;
import iti.jets.java.homenursing.dto.catalog.MedicationResponse;
import iti.jets.java.homenursing.entity.Medication;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class MedicationMapperTest {

    private final MedicationMapper mapper = Mappers.getMapper(MedicationMapper.class);

    @Test
    void toEntity_mapsAllRequestFields() {
        MedicationRequest request = new MedicationRequest("Metformin");

        Medication entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo("Metformin");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_nullRequest_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 1, 11, 30);
        Medication medication = Medication.builder()
                .id(id)
                .name("Insulin")
                .source(CatalogSource.USER)
                .createdAt(createdAt)
                .build();

        MedicationResponse response = mapper.toResponse(medication);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Insulin");
        assertThat(response.source()).isEqualTo(CatalogSource.USER);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toResponse_nullMedication_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
