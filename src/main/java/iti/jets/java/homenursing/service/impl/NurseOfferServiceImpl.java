package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NearbyNurseOfferResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferUpdateRequest;
import iti.jets.java.homenursing.dto.reservation.ReservationEvent;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseOffer;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.enums.NotificationType;
import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.NurseOfferMapper;
import iti.jets.java.homenursing.repository.NurseOfferRepository;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.NurseServiceRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.service.NotificationService;
import iti.jets.java.homenursing.service.NurseOfferService;
import iti.jets.java.homenursing.util.AfterCommitExecutor;
import iti.jets.java.homenursing.util.HaversineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NurseOfferServiceImpl implements NurseOfferService {

    private final NurseOfferRepository nurseOfferRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final NurseRepository nurseRepository;
    private final NurseServiceRepository nurseServiceRepository;
    private final NurseOfferMapper nurseOfferMapper;
    private final WebSocketPresenceService webSocketPresenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final AfterCommitExecutor afterCommitExecutor;

    private static final String RESERVATION_TOPIC_PREFIX = "/topic/reservation/";

    @Value("${nearby.nurses.radius-km:10}")
    private double nearbyNursesRadiusKm;

    @Override
    @Transactional
    public NurseOfferResponse create(UUID userId, NurseOfferRequest request) {
        ServiceRequest serviceRequest = serviceRequestRepository.findByIdAndIsDeletedFalse(request.serviceRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + request.serviceRequestId()));
        Nurse nurse = nurseRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse profile not found"));

        if (serviceRequest.getStatus() != ServiceRequestStatus.SEARCHING || serviceRequest.getNurse() != null) {
            throw new BadRequestException("This service request is not accepting offers");
        }
        if (nurse.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new BadRequestException("Nurse is not eligible to create offers");
        }
        boolean providesRequestedService = nurseServiceRepository
                .findByNurse_IdAndServiceType_Id(nurse.getId(), serviceRequest.getServiceType().getId())
                .map(nurseService -> Boolean.TRUE.equals(nurseService.getIsActive()))
                .orElse(false);
        if (!providesRequestedService) {
            throw new BadRequestException("Nurse does not provide the requested service");
        }
        if (nurseOfferRepository.existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalse(
                serviceRequest.getId(), userId)) {
            throw new BadRequestException("Nurse has already submitted an offer for this service request");
        }

        NurseOffer offer = nurseOfferMapper.toEntity(request);
        offer.setServiceRequest(serviceRequest);
        offer.setNurse(nurse);
        offer.setStatus(NurseOfferStatus.PENDING);
        offer.setIsDeleted(false);
        NurseOfferResponse response = nurseOfferMapper.toResponse(nurseOfferRepository.save(offer));

        pushEvent(serviceRequest.getId(), "OFFER_CREATED", response);
        notifyPatient(serviceRequest, "New Offer Received",
                "A nurse has submitted an offer for your service request.");

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NurseOfferResponse> listByServiceRequest(UUID serviceRequestId, UUID userId) {
        getAuthorizedServiceRequest(serviceRequestId, userId);
        return nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(serviceRequestId).stream()
                .map(nurseOfferMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyNurseOfferResponse> listNearbyByServiceRequest(UUID serviceRequestId, UUID userId) {
        ServiceRequest serviceRequest = getAuthorizedServiceRequest(serviceRequestId, userId);
        if (serviceRequest.getLatitude() == null || serviceRequest.getLongitude() == null) {
            throw new BadRequestException("Service request location is unavailable");
        }

        return nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(serviceRequestId)
                .stream()
                .map(offer -> toNearbyResponse(offer, serviceRequest))
                .flatMap(Optional::stream)
                .filter(offer -> offer.distanceKm() <= nearbyNursesRadiusKm)
                .sorted(Comparator.comparingDouble(NearbyNurseOfferResponse::distanceKm))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NurseOfferResponse get(UUID id, UUID userId) {
        return nurseOfferMapper.toResponse(getAuthorizedOffer(id, userId));
    }

    @Override
    @Transactional
    public NurseOfferResponse accept(UUID id, UUID userId) {
        NurseOffer offer = nurseOfferRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse offer not found: " + id));
        ServiceRequest serviceRequest = offer.getServiceRequest();

        boolean isPatient = serviceRequest.getProfile().getUser().getId().equals(userId);
        boolean isNurse = offer.getNurse().getUser().getId().equals(userId);
        if (!isPatient && !isNurse) {
            throw new ResourceNotFoundException("Nurse offer not found: " + id);
        }
        if (offer.getStatus() != NurseOfferStatus.PENDING) {
            throw new BadRequestException("Only pending offers can be accepted");
        }
        if (serviceRequest.getNurse() != null) {
            throw new BadRequestException("A nurse has already been selected for this service request");
        }

        offer.setStatus(NurseOfferStatus.ACCEPTED);
        serviceRequest.setNurse(offer.getNurse());
        serviceRequest.setStatus(ServiceRequestStatus.ACCEPTED);

        nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(serviceRequest.getId())
                .stream()
                .filter(otherOffer -> !otherOffer.getId().equals(offer.getId()))
                .filter(otherOffer -> otherOffer.getStatus() == NurseOfferStatus.PENDING)
                .forEach(otherOffer -> otherOffer.setStatus(NurseOfferStatus.REJECTED));

        NurseOfferResponse response = nurseOfferMapper.toResponse(nurseOfferRepository.save(offer));

        pushEvent(serviceRequest.getId(), "OFFER_ACCEPTED", response);
        notifyPatient(serviceRequest, "Offer Accepted",
                "An offer has been accepted. Reservation confirmed!");
        if (offer.getNurse() != null && offer.getNurse().getUser() != null) {
            UUID nurseUserId = offer.getNurse().getUser().getId();
            notificationService.create(new NotificationRequest(
                    nurseUserId,
                    "Offer Accepted",
                    "An offer has been accepted. Reservation confirmed!",
                    NotificationType.BOOKING,
                    "SERVICE_REQUEST",
                    serviceRequest.getId()));
        }

        return response;
    }

    @Override
    @Transactional
    public NurseOfferResponse update(UUID id, UUID userId, NurseOfferUpdateRequest request) {
        NurseOffer offer = getOwnedOffer(id, userId);
        if (offer.getStatus() != NurseOfferStatus.PENDING) {
            throw new BadRequestException("Only pending offers can be updated");
        }
        if (request.proposedPrice() != null) {
            offer.setProposedPrice(request.proposedPrice());
        }
        if (request.proposedDate() != null) {
            offer.setProposedDate(request.proposedDate());
        }
        if (request.proposedTime() != null) {
            offer.setProposedTime(request.proposedTime());
        }
        if (request.message() != null) {
            offer.setMessage(request.message());
        }
        NurseOfferResponse response = nurseOfferMapper.toResponse(nurseOfferRepository.save(offer));

        pushEvent(response.serviceRequestId(), "OFFER_UPDATED", response);
        notifyPatient(offer.getServiceRequest(), "Offer Terms Updated",
                "The nurse has updated their offer terms.");

        return response;
    }

    @Override
    @Transactional
    public NurseOfferResponse counterOffer(UUID id, UUID userId, NurseOfferUpdateRequest request) {
        NurseOffer offer = getAuthorizedOffer(id, userId);
        if (!offer.getServiceRequest().getProfile().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Nurse offer not found: " + id);
        }
        if (offer.getStatus() != NurseOfferStatus.PENDING) {
            throw new BadRequestException("Only pending offers can be countered");
        }
        if (request.proposedPrice() != null) {
            offer.setProposedPrice(request.proposedPrice());
        }
        if (request.proposedDate() != null) {
            offer.setProposedDate(request.proposedDate());
        }
        if (request.proposedTime() != null) {
            offer.setProposedTime(request.proposedTime());
        }
        if (request.message() != null) {
            offer.setMessage(request.message());
        }
        NurseOfferResponse response = nurseOfferMapper.toResponse(nurseOfferRepository.save(offer));

        pushEvent(response.serviceRequestId(), "OFFER_COUNTERED", response);
        UUID nurseUserId = offer.getNurse().getUser().getId();
        notificationService.create(new NotificationRequest(
                nurseUserId,
                "Counter-Offer Received",
                "The patient has proposed new terms.",
                NotificationType.BOOKING,
                "SERVICE_REQUEST",
                response.serviceRequestId()));

        return response;
    }

    @Override
    @Transactional
    public void reject(UUID id, UUID userId) {
        NurseOffer offer = getAuthorizedOffer(id, userId);
        if (!offer.getServiceRequest().getProfile().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Nurse offer not found: " + id);
        }
        if (offer.getStatus() != NurseOfferStatus.PENDING) {
            throw new BadRequestException("Only pending offers can be rejected");
        }
        offer.setStatus(NurseOfferStatus.REJECTED);
        nurseOfferRepository.save(offer);

        pushEvent(offer.getServiceRequest().getId(), "OFFER_REJECTED", Map.of("offerId", id));
        UUID nurseUserId = offer.getNurse().getUser().getId();
        notificationService.create(new NotificationRequest(
                nurseUserId,
                "Offer Rejected",
                "The patient has rejected your offer.",
                NotificationType.BOOKING,
                "SERVICE_REQUEST",
                offer.getServiceRequest().getId()));
    }

    @Override
    @Transactional
    public void withdraw(UUID id, UUID userId) {
        NurseOffer offer = getOwnedOffer(id, userId);
        if (offer.getStatus() != NurseOfferStatus.PENDING) {
            throw new BadRequestException("Only pending offers can be withdrawn");
        }
        offer.setStatus(NurseOfferStatus.WITHDRAWN);
        nurseOfferRepository.save(offer);

        pushEvent(offer.getServiceRequest().getId(), "OFFER_WITHDRAWN", Map.of("offerId", id));
        notifyPatient(offer.getServiceRequest(), "Offer Withdrawn",
                "A nurse has withdrawn their offer.");
    }

    private ServiceRequest getAuthorizedServiceRequest(UUID serviceRequestId, UUID userId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + serviceRequestId));
        boolean isOwner = serviceRequest.getProfile().getUser().getId().equals(userId);
        boolean isNurse = nurseRepository.existsByUser_Id(userId)
                && serviceRequest.getNurse() != null
                && serviceRequest.getNurse().getUser().getId().equals(userId);
        if (!isOwner && !isNurse) {
            throw new ResourceNotFoundException("Service request not found: " + serviceRequestId);
        }
        return serviceRequest;
    }

    private NurseOffer getAuthorizedOffer(UUID id, UUID userId) {
        NurseOffer offer = nurseOfferRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse offer not found: " + id));
        boolean isRequestOwner = offer.getServiceRequest().getProfile().getUser().getId().equals(userId);
        boolean isOfferingNurse = offer.getNurse().getUser().getId().equals(userId);
        if (!isRequestOwner && !isOfferingNurse) {
            throw new ResourceNotFoundException("Nurse offer not found: " + id);
        }
        return offer;
    }

    private NurseOffer getOwnedOffer(UUID id, UUID userId) {
        NurseOffer offer = getAuthorizedOffer(id, userId);
        if (!offer.getNurse().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Nurse offer not found: " + id);
        }
        return offer;
    }

    private Optional<NearbyNurseOfferResponse> toNearbyResponse(
            NurseOffer offer, ServiceRequest serviceRequest) {
        return webSocketPresenceService
                .getAvailableLocation(offer.getNurse().getUser().getId().toString())
                .map(location -> buildNearbyResponse(offer, serviceRequest, location));
    }

    private NearbyNurseOfferResponse buildNearbyResponse(
            NurseOffer offer, ServiceRequest serviceRequest, Point nurseLocation) {
        BigDecimal nurseLatitude = BigDecimal.valueOf(nurseLocation.getY());
        BigDecimal nurseLongitude = BigDecimal.valueOf(nurseLocation.getX());
        double distanceKm = HaversineUtil.distanceKm(
                serviceRequest.getLatitude(),
                serviceRequest.getLongitude(),
                nurseLatitude,
                nurseLongitude);

        return new NearbyNurseOfferResponse(
                offer.getId(),
                serviceRequest.getId(),
                offer.getNurse().getId(),
                offer.getProposedPrice(),
                offer.getProposedDate(),
                offer.getProposedTime(),
                offer.getMessage(),
                offer.getStatus(),
                nurseLatitude,
                nurseLongitude,
                distanceKm,
                offer.getCreatedAt(),
                offer.getUpdatedAt());
    }

    private void pushEvent(UUID reservationId, String type, Object data) {
        afterCommitExecutor.execute(() -> messagingTemplate.convertAndSend(
                RESERVATION_TOPIC_PREFIX + reservationId,
                new ReservationEvent(type, reservationId, data)));
    }

    private void notifyPatient(ServiceRequest serviceRequest, String title, String message) {
        if (serviceRequest == null || serviceRequest.getProfile() == null
                || serviceRequest.getProfile().getUser() == null) {
            return;
        }
        UUID patientUserId = serviceRequest.getProfile().getUser().getId();
        notificationService.create(new NotificationRequest(
                patientUserId,
                title,
                message,
                NotificationType.BOOKING,
                "SERVICE_REQUEST",
                serviceRequest.getId()));
    }
}
