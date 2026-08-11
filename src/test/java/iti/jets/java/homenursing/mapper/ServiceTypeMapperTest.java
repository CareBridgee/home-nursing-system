package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.catalog.ServiceTypeRequest;
import iti.jets.java.homenursing.dto.catalog.ServiceTypeResponse;
import iti.jets.java.homenursing.entity.ServiceType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ServiceTypeMapperTest {

    private final ServiceTypeMapper mapper = Mappers.getMapper(ServiceTypeMapper.class);

    @Test
    void toEntity_mapsAllRequestFields() {
        ServiceTypeRequest request = new ServiceTypeRequest(
                "General Nursing",
                "Basic nursing care",
                "Nursing",
                60,
                90,
                new BigDecimal("150.00"),
                List.of("Vitals check", "Medication reminder"),
                "Bring a towel",
                null
        );

        ServiceType entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo("General Nursing");
        assertThat(entity.getDescription()).isEqualTo("Basic nursing care");
        assertThat(entity.getCategory()).isEqualTo("Nursing");
        assertThat(entity.getMinimumDurationMinutes()).isEqualTo(60);
        assertThat(entity.getEstimatedDurationMinutes()).isEqualTo(90);
        assertThat(entity.getBasePrice()).isEqualByComparingTo("150.00");
        assertThat(entity.getIncludedItems()).containsExactly("Vitals check", "Medication reminder");
        assertThat(entity.getPreparationNote()).isEqualTo("Bring a towel");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_nullIncludedItems_keepsEmptyList() {
        ServiceTypeRequest request = new ServiceTypeRequest(
                "X", null, null, null, null, null, null, null, null
        );

        ServiceType entity = mapper.toEntity(request);

        assertThat(entity.getIncludedItems()).isNotNull().isEmpty();
    }

    @Test
    void toEntity_nullRequest_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 3, 13, 20);
        ServiceType serviceType = ServiceType.builder()
                .id(id)
                .name("Physiotherapy")
                .description("Rehabilitation sessions")
                .imageUrl("https://img.example/physio.png")
                .category("Rehabilitation")
                .minimumDurationMinutes(45)
                .estimatedDurationMinutes(60)
                .basePrice(new BigDecimal("200.00"))
                .includedItems(List.of("Session 1", "Session 2"))
                .preparationNote("Wear loose clothes")
                .createdAt(createdAt)
                .build();

        ServiceTypeResponse response = mapper.toResponse(serviceType);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Physiotherapy");
        assertThat(response.description()).isEqualTo("Rehabilitation sessions");
        assertThat(response.imageUrl()).isEqualTo("https://img.example/physio.png");
        assertThat(response.category()).isEqualTo("Rehabilitation");
        assertThat(response.minimumDurationMinutes()).isEqualTo(45);
        assertThat(response.estimatedDurationMinutes()).isEqualTo(60);
        assertThat(response.basePrice()).isEqualByComparingTo("200.00");
        assertThat(response.includedItems()).containsExactly("Session 1", "Session 2");
        assertThat(response.preparationNote()).isEqualTo("Wear loose clothes");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toResponse_nullIncludedItems_yieldsNullList() {
        ServiceType serviceType = ServiceType.builder().includedItems(null).build();

        assertThat(mapper.toResponse(serviceType).includedItems()).isNull();
    }

    @Test
    void toResponse_nullServiceType_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
