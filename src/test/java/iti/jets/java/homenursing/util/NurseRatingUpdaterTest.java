package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.ReviewRating;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.ReviewRatingRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NurseRatingUpdaterTest {

    @Mock
    private NurseRepository nurseRepository;
    @Mock
    private ReviewRatingRepository reviewRatingRepository;

    @InjectMocks
    private NurseRatingUpdater updater;

    private static ReviewRating rating(int value) {
        return ReviewRating.builder().rating(value).build();
    }

    private static Nurse nurse(UUID id) {
        return Nurse.builder().id(id).build();
    }

    private void stubReviews(UUID nurseId, List<ReviewRating> reviews) {
        when(reviewRatingRepository.findByNurseId(nurseId, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(reviews));
    }

    private void stubNurse(UUID nurseId, Nurse nurse) {
        when(nurseRepository.findById(nurseId)).thenReturn(Optional.ofNullable(nurse));
    }

    @Test
    void onReviewCreatedRecalculatesAverageAndCount() {
        UUID nurseId = UUID.randomUUID();
        Nurse nurse = nurse(nurseId);
        stubReviews(nurseId, List.of(rating(5), rating(4)));
        stubNurse(nurseId, nurse);

        updater.onReviewCreated(nurse, 5);

        assertThat(nurse.getTotalReviews()).isEqualTo(2);
        assertThat(nurse.getRatingAvg()).isEqualByComparingTo("4.50");
        verify(nurseRepository).save(nurse);
    }

    @Test
    void onReviewUpdatedRecalculatesAverageAndCount() {
        UUID nurseId = UUID.randomUUID();
        Nurse nurse = nurse(nurseId);
        stubReviews(nurseId, List.of(rating(5), rating(3), rating(3)));
        stubNurse(nurseId, nurse);

        updater.onReviewUpdated(nurse, 5, 3);

        assertThat(nurse.getTotalReviews()).isEqualTo(3);
        assertThat(nurse.getRatingAvg()).isEqualByComparingTo("3.67");
        verify(nurseRepository).save(nurse);
    }

    @Test
    void onReviewDeletedRecalculatesAverageAndCount() {
        UUID nurseId = UUID.randomUUID();
        Nurse nurse = nurse(nurseId);
        stubReviews(nurseId, List.of(rating(4), rating(1)));
        stubNurse(nurseId, nurse);

        updater.onReviewDeleted(nurse, 4);

        assertThat(nurse.getTotalReviews()).isEqualTo(2);
        assertThat(nurse.getRatingAvg()).isEqualByComparingTo("2.50");
        verify(nurseRepository).save(nurse);
    }

    @Test
    void noReviewsResetsAverageToZeroAtScaleTwo() {
        UUID nurseId = UUID.randomUUID();
        Nurse nurse = nurse(nurseId);
        stubReviews(nurseId, List.of());
        stubNurse(nurseId, nurse);

        updater.onReviewCreated(nurse, 5);

        assertThat(nurse.getTotalReviews()).isZero();
        assertThat(nurse.getRatingAvg()).isEqualByComparingTo("0.00");
        assertThat(nurse.getRatingAvg().scale()).isEqualTo(2);
        verify(nurseRepository).save(nurse);
    }

    @Test
    void missingNurseSkipsSave() {
        UUID nurseId = UUID.randomUUID();
        Nurse nurse = nurse(nurseId);
        stubReviews(nurseId, List.of(rating(5), rating(4)));
        stubNurse(nurseId, null);

        updater.onReviewCreated(nurse, 5);

        verify(nurseRepository, never()).save(any(Nurse.class));
    }

    @Test
    void averageIsRoundedHalfUp() {
        UUID nurseId = UUID.randomUUID();
        Nurse nurse = nurse(nurseId);
        stubReviews(nurseId, List.of(rating(5), rating(4), rating(4)));
        stubNurse(nurseId, nurse);

        updater.onReviewUpdated(nurse, 5, 4);

        assertThat(nurse.getRatingAvg()).isEqualByComparingTo("4.33");
    }

    @Test
    void averageOfPerfectFiveKeepsBigDecimalScale() {
        UUID nurseId = UUID.randomUUID();
        Nurse nurse = nurse(nurseId);
        stubReviews(nurseId, List.of(rating(5), rating(5)));
        stubNurse(nurseId, nurse);

        updater.onReviewDeleted(nurse, 5);

        assertThat(nurse.getRatingAvg()).isEqualByComparingTo("5.00");
        assertThat(nurse.getRatingAvg().scale()).isEqualTo(2);
    }

    @Test
    void emptyRatingListUsesUnpagedPageable() {
        UUID nurseId = UUID.randomUUID();
        Nurse nurse = nurse(nurseId);
        stubReviews(nurseId, List.of());
        stubNurse(nurseId, nurse);

        updater.onReviewCreated(nurse, 5);

        verify(reviewRatingRepository).findByNurseId(nurseId, Pageable.unpaged());
    }
}
