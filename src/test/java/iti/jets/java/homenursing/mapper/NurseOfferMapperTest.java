package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.nurse.NurseSummaryResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseOffer;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class NurseOfferMapperTest {

    private final NurseOfferMapper mapper = Mappers.getMapper(NurseOfferMapper.class);

    @Test
    void toEntity_mapsAllRequestFields() {
        NurseOfferRequest request = new NurseOfferRequest(
                UUID.randomUUID(),
                new BigDecimal("250.00"),
                LocalDate.of(2026, 8, 20),
                LocalTime.of(10, 0),
                "I can come in the morning"
        );

        NurseOffer entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getProposedPrice()).isEqualByComparingTo("250.00");
        assertThat(entity.getProposedDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(entity.getProposedTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(entity.getMessage()).isEqualTo("I can come in the morning");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getServiceRequest()).isNull();
        assertThat(entity.getNurse()).isNull();
        assertThat(entity.getStatus()).isEqualTo(NurseOfferStatus.PENDING);
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_nullRequest_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID serviceRequestId = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 10, 9, 0);
        LocalDateTime updatedAt = createdAt.plusHours(2);
        User user = User.builder()
                .id(userId)
                .firstName("Heba")
                .lastName("Youssef")
                .profileImageUrl("https://img.example/heba.png")
                .build();
        Nurse nurse = Nurse.builder()
                .id(nurseId)
                .user(user)
                .ratingAvg(new BigDecimal("4.80"))
                .totalReviews(12)
                .build();
        NurseOffer offer = NurseOffer.builder()
                .id(id)
                .serviceRequest(ServiceRequest.builder().id(serviceRequestId).build())
                .nurse(nurse)
                .proposedPrice(new BigDecimal("300.00"))
                .proposedDate(LocalDate.of(2026, 8, 21))
                .proposedTime(LocalTime.of(14, 30))
                .message("Available at 2:30 pm")
                .status(NurseOfferStatus.PENDING)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        NurseOfferResponse response = mapper.toResponse(offer);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.serviceRequestId()).isEqualTo(serviceRequestId);
        assertThat(response.nurse()).isNotNull();
        assertThat(response.nurse().id()).isEqualTo(nurseId);
        assertThat(response.nurse().firstName()).isEqualTo("Heba");
        assertThat(response.nurse().lastName()).isEqualTo("Youssef");
        assertThat(response.nurse().profileImageUrl()).isEqualTo("https://img.example/heba.png");
        assertThat(response.nurse().ratingAvg()).isEqualByComparingTo("4.80");
        assertThat(response.nurse().totalReviews()).isEqualTo(12);
        assertThat(response.proposedPrice()).isEqualByComparingTo("300.00");
        assertThat(response.proposedDate()).isEqualTo(LocalDate.of(2026, 8, 21));
        assertThat(response.proposedTime()).isEqualTo(LocalTime.of(14, 30));
        assertThat(response.message()).isEqualTo("Available at 2:30 pm");
        assertThat(response.status()).isEqualTo(NurseOfferStatus.PENDING);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
        assertThat(response.distanceKm()).isNull();
        assertThat(response.serviceTypeName()).isNull();
        assertThat(response.estimatedDurationMinutes()).isNull();
    }

    @Test
    void toResponse_nullServiceRequest_yieldsNullServiceRequestId() {
        NurseOffer offer = NurseOffer.builder().serviceRequest(null).build();

        assertThat(mapper.toResponse(offer).serviceRequestId()).isNull();
    }

    @Test
    void toResponse_nullNurse_yieldsNullNurseSummary() {
        NurseOffer offer = NurseOffer.builder().nurse(null).build();

        assertThat(mapper.toResponse(offer).nurse()).isNull();
    }

    @Test
    void toResponse_nullOffer_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toNurseSummary_mapsAllFields() {
        UUID nurseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .firstName("Ahmed")
                .lastName("Farouk")
                .profileImageUrl("https://img.example/ahmed.png")
                .build();
        Nurse nurse = Nurse.builder()
                .id(nurseId)
                .user(user)
                .ratingAvg(new BigDecimal("4.50"))
                .totalReviews(8)
                .build();

        NurseSummaryResponse response = mapper.toNurseSummary(nurse);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(nurseId);
        assertThat(response.firstName()).isEqualTo("Ahmed");
        assertThat(response.lastName()).isEqualTo("Farouk");
        assertThat(response.profileImageUrl()).isEqualTo("https://img.example/ahmed.png");
        assertThat(response.ratingAvg()).isEqualByComparingTo("4.50");
        assertThat(response.totalReviews()).isEqualTo(8);
    }

    @Test
    void toNurseSummary_nullUser_yieldsNullUserFields() {
        Nurse nurse = Nurse.builder().user(null).build();

        NurseSummaryResponse response = mapper.toNurseSummary(nurse);

        assertThat(response.firstName()).isNull();
        assertThat(response.lastName()).isNull();
        assertThat(response.profileImageUrl()).isNull();
    }

    @Test
    void toNurseSummary_nullNurse_returnsNull() {
        assertThat(mapper.toNurseSummary(null)).isNull();
    }
}
