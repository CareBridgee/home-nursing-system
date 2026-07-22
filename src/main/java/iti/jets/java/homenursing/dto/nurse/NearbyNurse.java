package iti.jets.java.homenursing.dto.nurse;

import java.math.BigDecimal;
import java.util.UUID;

public record NearbyNurse(
        UUID nurseId,
        BigDecimal latitude,
        BigDecimal longitude,
        double distanceKm
) {
}
