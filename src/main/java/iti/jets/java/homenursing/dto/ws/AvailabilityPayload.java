package iti.jets.java.homenursing.dto.ws;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AvailabilityPayload(
        @NotNull Boolean available,
        BigDecimal lat,
        BigDecimal lng
) {
    @AssertTrue(message = "lat and lng are required when going available")
    public boolean isLocationPresentWhenAvailable() {
        if (Boolean.TRUE.equals(available)) {
            return lat != null && lng != null;
        }
        return true;
    }
}