package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.repository.NurseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class NurseRatingUpdater {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final NurseRepository nurseRepository;

    public void onReviewCreated(Nurse nurse, int rating) {
        int count = defaultCount(nurse.getTotalReviews());
        BigDecimal avg = defaultAvg(nurse.getRatingAvg());

        int newCount = count + 1;
        BigDecimal newAvg = avg.multiply(BigDecimal.valueOf(count))
                .add(BigDecimal.valueOf(rating))
                .divide(BigDecimal.valueOf(newCount), SCALE, ROUNDING);

        nurse.setTotalReviews(newCount);
        nurse.setRatingAvg(newAvg);
        nurseRepository.save(nurse);
    }

    public void onReviewUpdated(Nurse nurse, int oldRating, int newRating) {
        if (oldRating == newRating) {
            return;
        }

        int count = defaultCount(nurse.getTotalReviews());
        if (count == 0) {
            return;
        }

        BigDecimal avg = defaultAvg(nurse.getRatingAvg());
        BigDecimal newAvg = avg.multiply(BigDecimal.valueOf(count))
                .subtract(BigDecimal.valueOf(oldRating))
                .add(BigDecimal.valueOf(newRating))
                .divide(BigDecimal.valueOf(count), SCALE, ROUNDING);

        nurse.setRatingAvg(newAvg);
        nurseRepository.save(nurse);
    }

    public void onReviewDeleted(Nurse nurse, int rating) {
        int count = defaultCount(nurse.getTotalReviews());
        if (count <= 0) {
            return;
        }

        int newCount = count - 1;
        BigDecimal avg = defaultAvg(nurse.getRatingAvg());
        BigDecimal newAvg = newCount == 0
                ? BigDecimal.ZERO.setScale(SCALE, ROUNDING)
                : avg.multiply(BigDecimal.valueOf(count))
                        .subtract(BigDecimal.valueOf(rating))
                        .divide(BigDecimal.valueOf(newCount), SCALE, ROUNDING);

        nurse.setTotalReviews(newCount);
        nurse.setRatingAvg(newAvg);
        nurseRepository.save(nurse);
    }

    private int defaultCount(Integer count) {
        return count != null ? count : 0;
    }

    private BigDecimal defaultAvg(BigDecimal avg) {
        return avg != null ? avg : BigDecimal.ZERO;
    }
}
