package iti.jets.java.homenursing.dto.ws;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LocationPayload(
        @NotNull BigDecimal lat,
        @NotNull BigDecimal lng
) {
}