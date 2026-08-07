package iti.jets.java.homenursing.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class PriceEstimator {

    @Value("${nearby.nurses.included-distance-km:5}")
    private double includedDistanceKm;

    @Value("${nearby.nurses.price-per-km:12}")
    private BigDecimal pricePerKm;

    public BigDecimal estimate(BigDecimal basePrice, double distanceKm) {
        if (basePrice == null) {
            log.debug("Cannot estimate price: service type has no base price");
            return null;
        }
        double extraDistanceKm = Math.max(0, distanceKm - includedDistanceKm);
        return basePrice
                .add(pricePerKm.multiply(BigDecimal.valueOf(extraDistanceKm)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}