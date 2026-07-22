package iti.jets.java.homenursing.dto.servicerequest;

import iti.jets.java.homenursing.dto.nurse.NearbyNurse;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record NearbyServiceRequestResponse(
        UUID serviceRequestId,
        UUID profileId,
        UUID serviceTypeId,
        ServiceRequestStatus status,
        BigDecimal latitude,
        BigDecimal longitude,
        List<NearbyNurse> nearbyNurses,
        LocalDateTime createdAt
) {
}
