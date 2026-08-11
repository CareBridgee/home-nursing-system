package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.ReviewRating;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.ReviewRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NurseRatingUpdater {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final NurseRepository nurseRepository;
    private final ReviewRatingRepository reviewRatingRepository;

    public void onReviewCreated(Nurse nurse, int rating) {
        recalculate(nurse.getId());
    }

    public void onReviewUpdated(Nurse nurse, int oldRating, int newRating) {
        recalculate(nurse.getId());
    }

    public void onReviewDeleted(Nurse nurse, int rating) {
        recalculate(nurse.getId());
    }

    private void recalculate(UUID nurseId) {
        List<ReviewRating> reviews = reviewRatingRepository.findByNurseId(nurseId, Pageable.unpaged()).getContent();
        int count = reviews.size();
        BigDecimal avg = count == 0
                ? BigDecimal.ZERO.setScale(SCALE, ROUNDING)
                : BigDecimal.valueOf(reviews.stream()
                        .map(ReviewRating::getRating)
                        .reduce(0, Integer::sum))
                        .divide(BigDecimal.valueOf(count), SCALE, ROUNDING);

        nurseRepository.findById(nurseId).ifPresent(nurse -> {
            nurse.setTotalReviews(count);
            nurse.setRatingAvg(avg);
            nurseRepository.save(nurse);
        });
    }
}
