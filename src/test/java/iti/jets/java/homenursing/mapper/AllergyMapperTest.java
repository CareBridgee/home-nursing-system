package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.catalog.AllergyRequest;
import iti.jets.java.homenursing.dto.catalog.AllergyResponse;
import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.entity.enums.AllergyType;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class AllergyMapperTest {

    private final AllergyMapper mapper = Mappers.getMapper(AllergyMapper.class);

    @Test
    void toEntity_mapsAllRequestFields() {
        AllergyRequest request = new AllergyRequest("Penicillin", AllergyType.DRUG);

        Allergy entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo("Penicillin");
        assertThat(entity.getType()).isEqualTo(AllergyType.DRUG);
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
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 5, 9, 0);
        Allergy allergy = Allergy.builder()
                .id(id)
                .name("Peanuts")
                .type(AllergyType.FOOD)
                .source(CatalogSource.USER)
                .createdAt(createdAt)
                .build();

        AllergyResponse response = mapper.toResponse(allergy);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Peanuts");
        assertThat(response.type()).isEqualTo(AllergyType.FOOD);
        assertThat(response.source()).isEqualTo(CatalogSource.USER);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toResponse_nullAllergy_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
