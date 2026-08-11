package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.review.ReviewRatingRequest;
import iti.jets.java.homenursing.dto.review.ReviewRatingResponse;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ReviewRating;
import iti.jets.java.homenursing.entity.ServiceRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ReviewRatingMapperTest {

    private final ReviewRatingMapper mapper = Mappers.getMapper(ReviewRatingMapper.class);

    @Test
    void toEntity_mapsAllRequestFields() {
        ReviewRatingRequest request = ReviewRatingRequest.builder()
                .serviceRequestId(UUID.randomUUID())
                .rating(5)
                .reviewText("Excellent nurse")
                .isAnonymous(true)
                .build();

        ReviewRating entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getRating()).isEqualTo(5);
        assertThat(entity.getReviewText()).isEqualTo("Excellent nurse");
        assertThat(entity.getIsAnonymous()).isTrue();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getServiceRequest()).isNull();
        assertThat(entity.getProfile()).isNull();
        assertThat(entity.getNurse()).isNull();
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
        UUID profileId = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 7, 7, 7);
        LocalDateTime updatedAt = createdAt.plusHours(1);
        ReviewRating review = ReviewRating.builder()
                .id(id)
                .serviceRequest(ServiceRequest.builder().id(serviceRequestId).build())
                .profile(Profile.builder().id(profileId).build())
                .nurse(Nurse.builder().id(nurseId).build())
                .rating(4)
                .reviewText("Very professional")
                .isAnonymous(false)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        ReviewRatingResponse response = mapper.toResponse(review);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getServiceRequestId()).isEqualTo(serviceRequestId);
        assertThat(response.getProfileId()).isEqualTo(profileId);
        assertThat(response.getNurseId()).isEqualTo(nurseId);
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getReviewText()).isEqualTo("Very professional");
        assertThat(response.getIsAnonymous()).isFalse();
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toResponse_nullServiceRequest_yieldsNullServiceRequestId() {
        ReviewRating review = ReviewRating.builder().serviceRequest(null).build();

        assertThat(mapper.toResponse(review).getServiceRequestId()).isNull();
    }

    @Test
    void toResponse_nullProfile_yieldsNullProfileId() {
        ReviewRating review = ReviewRating.builder().profile(null).build();

        assertThat(mapper.toResponse(review).getProfileId()).isNull();
    }

    @Test
    void toResponse_nullNurse_yieldsNullNurseId() {
        ReviewRating review = ReviewRating.builder().nurse(null).build();

        assertThat(mapper.toResponse(review).getNurseId()).isNull();
    }

    @Test
    void toResponse_nullReview_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
