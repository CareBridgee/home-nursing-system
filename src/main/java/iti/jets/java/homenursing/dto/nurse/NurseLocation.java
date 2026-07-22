package iti.jets.java.homenursing.dto.nurse;

import java.math.BigDecimal;
import java.util.UUID;

public record NurseLocation(
        UUID nurseId,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
