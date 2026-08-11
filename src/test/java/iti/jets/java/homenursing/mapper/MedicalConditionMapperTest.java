package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.catalog.MedicalConditionRequest;
import iti.jets.java.homenursing.dto.catalog.MedicalConditionResponse;
import iti.jets.java.homenursing.entity.MedicalCondition;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class MedicalConditionMapperTest {

    private final MedicalConditionMapper mapper = Mappers.getMapper(MedicalConditionMapper.class);

    @Test
    void toEntity_mapsAllRequestFields() {
        MedicalConditionRequest request = new MedicalConditionRequest("Diabetes", "Chronic condition");

        MedicalCondition entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo("Diabetes");
        assertThat(entity.getDescription()).isEqualTo("Chronic condition");
        assertThat(entity.getId()).isNull();
    }

    @Test
    void toEntity_nullRequest_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 2, 10, 8, 45);
        MedicalCondition condition = MedicalCondition.builder()
                .id(id)
                .name("Asthma")
                .description("Respiratory condition")
                .source(CatalogSource.ADMIN)
                .createdAt(createdAt)
                .build();

        MedicalConditionResponse response = mapper.toResponse(condition);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Asthma");
        assertThat(response.description()).isEqualTo("Respiratory condition");
        assertThat(response.source()).isEqualTo(CatalogSource.ADMIN);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toResponse_nullCondition_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
