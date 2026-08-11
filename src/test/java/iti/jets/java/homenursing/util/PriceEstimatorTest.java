package iti.jets.java.homenursing.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class PriceEstimatorTest {

    private PriceEstimator estimator;

    @BeforeEach
    void setUp() {
        estimator = new PriceEstimator();
        // @Value fields are only populated by Spring; inject the documented defaults
        // directly so the plain unit test does not need a container.
        ReflectionTestUtils.setField(estimator, "includedDistanceKm", 5.0);
        ReflectionTestUtils.setField(estimator, "pricePerKm", new BigDecimal("12"));
    }

    @Test
    void nullBasePriceReturnsNull() {
        assertThat(estimator.estimate(null, 10.0)).isNull();
    }

    @Test
    void distanceWithinIncludedRangeHasNoExtraCharge() {
        BigDecimal result = estimator.estimate(new BigDecimal("100"), 3.0);
        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void distanceExactlyAtIncludedLimitHasNoExtraCharge() {
        BigDecimal result = estimator.estimate(new BigDecimal("100"), 5.0);
        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void distanceAboveIncludedRangeAddsPricePerExtraKm() {
        BigDecimal result = estimator.estimate(new BigDecimal("100"), 10.0);
        assertThat(result).isEqualByComparingTo("160.00");
    }

    @Test
    void negativeDistanceIsClampedToZero() {
        BigDecimal result = estimator.estimate(new BigDecimal("100"), -3.0);
        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void resultIsRoundedHalfUpToTwoDecimals() {
        BigDecimal result = estimator.estimate(new BigDecimal("100.005"), 5.4);
        assertThat(result).isEqualByComparingTo("104.81");
    }

    @Test
    void resultRoundsDownWhenThirdDecimalIsFour() {
        BigDecimal result = estimator.estimate(new BigDecimal("100.004"), 5.4);
        assertThat(result).isEqualByComparingTo("104.80");
    }
}
