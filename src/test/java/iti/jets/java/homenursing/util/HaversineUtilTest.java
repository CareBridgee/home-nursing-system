package iti.jets.java.homenursing.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Tag("unit")
class HaversineUtilTest {

    private static final double EPSILON_KM = 0.5;

    @Test
    void distanceBetweenIdenticalPointsIsZero() {
        assertThat(HaversineUtil.distanceKm(30.0444, 31.2357, 30.0444, 31.2357)).isZero();
    }

    @Test
    void distanceBetweenCairoAndAlexandriaIsAbout180Km() {
        double distance = HaversineUtil.distanceKm(30.0444, 31.2357, 31.2001, 29.9187);
        assertThat(distance).isBetween(170.0, 190.0);
    }

    @Test
    void distanceBetweenAntipodalPointsIsAboutHalfEarthCircumference() {
        double distance = HaversineUtil.distanceKm(0, 0, 0, 180);
        assertThat(distance).isCloseTo(Math.PI * 6371.0, within(0.01));
    }

    @Test
    void distanceBetweenNorthAndSouthPoleIsAboutHalfEarthCircumference() {
        double distance = HaversineUtil.distanceKm(90, 0, -90, 0);
        assertThat(distance).isCloseTo(Math.PI * 6371.0, within(0.01));
    }

    @Test
    void bigDecimalOverloadDelegatesToPrimitiveMath() {
        BigDecimal lat1 = new BigDecimal("30.0444");
        BigDecimal lng1 = new BigDecimal("31.2357");
        BigDecimal lat2 = new BigDecimal("31.2001");
        BigDecimal lng2 = new BigDecimal("29.9187");

        double fromBigDecimal = HaversineUtil.distanceKm(lat1, lng1, lat2, lng2);
        double fromPrimitives = HaversineUtil.distanceKm(
                lat1.doubleValue(), lng1.doubleValue(), lat2.doubleValue(), lng2.doubleValue());

        assertThat(fromBigDecimal).isEqualTo(fromPrimitives);
        assertThat(fromBigDecimal).isBetween(170.0, 190.0);
    }

    @Test
    void distanceIsSymmetric() {
        double aToB = HaversineUtil.distanceKm(10.0, 20.0, -30.0, 40.0);
        double bToA = HaversineUtil.distanceKm(-30.0, 40.0, 10.0, 20.0);
        assertThat(aToB).isEqualTo(bToA);
    }
}
