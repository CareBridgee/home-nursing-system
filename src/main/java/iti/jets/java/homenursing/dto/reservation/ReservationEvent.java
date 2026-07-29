package iti.jets.java.homenursing.dto.reservation;

import java.util.UUID;

public record ReservationEvent(
        String type,
        UUID reservationId,
        Object data
) {
}
