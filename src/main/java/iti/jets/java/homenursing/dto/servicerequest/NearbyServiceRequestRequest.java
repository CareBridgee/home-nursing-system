package iti.jets.java.homenursing.dto.servicerequest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record NearbyServiceRequestRequest(
        @NotNull UUID profileId,
        @NotNull UUID serviceTypeId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        LocalDate preferredDate,
        LocalTime preferredTime,
        String serviceDescription
) {
}
