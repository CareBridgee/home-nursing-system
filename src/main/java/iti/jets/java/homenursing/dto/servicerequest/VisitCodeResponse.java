package iti.jets.java.homenursing.dto.servicerequest;

import java.time.Instant;
import java.util.UUID;

public record VisitCodeResponse(
        UUID serviceRequestId,
        String code,
        Instant expiresAt
) {
}
