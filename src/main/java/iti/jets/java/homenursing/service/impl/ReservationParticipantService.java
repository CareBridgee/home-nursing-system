package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReservationParticipantService {

    private final ServiceRequestRepository serviceRequestRepository;

    public ReservationParticipantService(ServiceRequestRepository serviceRequestRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
    }

    public boolean isParticipant(UUID reservationId, UUID userId) {
        return serviceRequestRepository.isParticipant(reservationId, userId);
    }
}
