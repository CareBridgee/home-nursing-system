package iti.jets.java.homenursing.dto.ws;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ServiceRequestIdPayload(
        @NotNull UUID serviceRequestId
) {
}