package iti.jets.java.homenursing.dto.chat;

import java.util.UUID;

/**
 * Immutable snapshot of a reservation draft gathered during the chat session.
 * The client supplies latitude/longitude (device GPS) separately when confirming.
 */
public record ReservationDraft(
        UUID serviceTypeId,
        String serviceTypeName,
        String serviceDescription,
        boolean complete
) {
    public static ReservationDraft empty() {
        return new ReservationDraft(null, null, null, false);
    }

    public boolean hasAnyData() {
        return serviceTypeId != null
                || (serviceDescription != null && !serviceDescription.isBlank());
    }
}
