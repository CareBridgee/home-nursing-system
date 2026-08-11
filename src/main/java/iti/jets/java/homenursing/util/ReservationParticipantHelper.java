package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReservationParticipantHelper {

    private final ServiceRequestRepository serviceRequestRepository;

    public ReservationParticipantHelper(ServiceRequestRepository serviceRequestRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
    }

    public boolean isParticipant(UUID reservationId, UUID userId) {
        return serviceRequestRepository.isParticipant(reservationId, userId);
    }
}
