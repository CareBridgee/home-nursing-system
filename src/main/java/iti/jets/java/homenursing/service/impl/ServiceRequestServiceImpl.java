package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.dto.nurse.NearbyNurse;
import iti.jets.java.homenursing.dto.nurse.NurseLocation;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.dto.reservation.ReservationEvent;
import iti.jets.java.homenursing.dto.servicerequest.NearbyNurseServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestRequest;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.PatientMedicalSummary;
import iti.jets.java.homenursing.dto.servicerequest.ServiceRequestDetailsResponse;
import iti.jets.java.homenursing.dto.servicerequest.ServiceRequestHistoryResponse;
import iti.jets.java.homenursing.dto.servicerequest.ServiceRequestNursePreviewResponse;
import iti.jets.java.homenursing.dto.servicerequest.ServiceRequestNurseProfileResponse;
import iti.jets.java.homenursing.dto.servicerequest.VisitCodeResponse;
import iti.jets.java.homenursing.entity.*;
import iti.jets.java.homenursing.entity.enums.NotificationType;
import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ForbiddenException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.NurseOfferMapper;
import iti.jets.java.homenursing.repository.AddressRepository;
import iti.jets.java.homenursing.repository.NurseOfferRepository;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.NurseServiceRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.service.NearbyNurseMatcher;
import iti.jets.java.homenursing.service.NurseLocationProvider;
import iti.jets.java.homenursing.service.NotificationService;
import iti.jets.java.homenursing.service.PriceEstimator;
import iti.jets.java.homenursing.service.ProfileService;
import iti.jets.java.homenursing.service.ServiceRequestService;
import iti.jets.java.homenursing.service.TokenService;
import iti.jets.java.homenursing.util.AfterCommitExecutor;
import iti.jets.java.homenursing.util.HaversineUtil;
import iti.jets.java.homenursing.util.ProfileImageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRequestServiceImpl implements ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final NurseOfferRepository nurseOfferRepository;
    private final NurseRepository nurseRepository;
    private final NurseServiceRepository nurseServiceRepository;
    private final NurseOfferMapper nurseOfferMapper;
    private final ProfileService profileService;
    private final NurseLocationProvider nurseLocationProvider;
    private final NearbyNurseMatcher nearbyNurseMatcher;
    private final PriceEstimator priceEstimator;
    private final WebSocketPresenceService webSocketPresenceService;
    private final TokenService tokenService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AfterCommitExecutor afterCommitExecutor;
    private final PatientMedicalSummaryAssembler patientMedicalSummaryAssembler;
    private final ServiceBriefBuilder serviceBriefBuilder;
    private final AddressRepository addressRepository;

    @Value("${nearby.nurses.radius-km:10}")
    private double nearbyNursesRadiusKm;

    @Value("${visit.code.ttl-hours:24}")
    private long visitCodeTtlHours;

    @Value("${visit.code.max-attempts:5}")
    private int visitCodeMaxAttempts;

    private static final String CODE_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String VISIT_CODE_PREFIX = "visitcode:";
    private static final String VISIT_CODE_ATTEMPTS_PREFIX = "visitcode_attempts:";
    private static final String RESERVATION_TOPIC_PREFIX = "/topic/reservation/";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<ServiceRequestStatus> ACTIVE_SERVICE_REQUEST_STATUSES = Set.of(
            ServiceRequestStatus.PENDING,
            ServiceRequestStatus.SEARCHING,
            ServiceRequestStatus.BOOKING,
            ServiceRequestStatus.NEGOTIATING,
            ServiceRequestStatus.ACCEPTED,
            ServiceRequestStatus.IN_PROGRESS);
    private static final Set<ServiceRequestStatus> OPEN_UNASSIGNED_STATUSES = Set.of(
            ServiceRequestStatus.PENDING,
            ServiceRequestStatus.SEARCHING,
            ServiceRequestStatus.BOOKING,
            ServiceRequestStatus.NEGOTIATING);
    private static final Set<ServiceRequestStatus> VISIT_STATUSES = Set.of(
            ServiceRequestStatus.ACCEPTED,
            ServiceRequestStatus.IN_PROGRESS);

    @Override
    @Transactional
    public NearbyServiceRequestResponse createRequest(NearbyServiceRequestRequest request) {
        Profile profile = profileService.getProfile(request.profileId());
        ServiceType serviceType = serviceTypeRepository.findById(request.serviceTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + request.serviceTypeId()));

        if (request.preferredDate() != null && request.preferredDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Preferred date must not be in the past");
        }
        if (request.preferredTime() != null) {
            LocalDate date = request.preferredDate() != null ? request.preferredDate() : LocalDate.now();
            if (date.equals(LocalDate.now()) && request.preferredTime().isBefore(LocalTime.now())) {
                throw new BadRequestException("Preferred time must not be in the past");
            }
        }
        if (serviceRequestRepository.existsByProfile_IdAndIsDeletedFalseAndStatusIn(
                profile.getId(), ACTIVE_SERVICE_REQUEST_STATUSES)) {
            throw new BadRequestException("This profile already has an active service request");
        }

        List<NearbyNurse> nearbyNurses = findNearbyNursesFor(serviceType, request.latitude(), request.longitude());

        String serviceDescription = request.serviceDescription();
        if (serviceDescription == null || serviceDescription.isBlank()) {
            serviceDescription = serviceBriefBuilder.build(profile.getId(), serviceType.getName());
        }

        ServiceRequest serviceRequest = ServiceRequest.builder()
                .profile(profile)
                .serviceType(serviceType)
                .serviceDescription(serviceDescription)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .status(ServiceRequestStatus.SEARCHING)
                .preferredDate(request.preferredDate())
                .preferredTime(request.preferredTime())
                .isDeleted(false)
                .build();

        ServiceRequest saved = serviceRequestRepository.saveAndFlush(serviceRequest);

        return new NearbyServiceRequestResponse(
                saved.getId(),
                profile.getId(),
                serviceType.getId(),
                saved.getStatus(),
                saved.getLatitude(),
                saved.getLongitude(),
                nearbyNurses,
                saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyNurseServiceRequestResponse> listNearbyForNurse(UUID userId) {
        Nurse nurse = nurseRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse profile not found"));

        if (nurse.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new BadRequestException("Nurse is not eligible to receive service requests");
        }

        Point nurseLocation = webSocketPresenceService.getAvailableLocation(userId.toString())
                .orElseThrow(() -> new BadRequestException("Nurse location is unavailable"));

        List<UUID> serviceTypeIds = nurseServiceRepository.findByNurse_IdAndIsActiveTrue(nurse.getId()).stream()
                .map(NurseService::getServiceType)
                .map(ServiceType::getId)
                .toList();

        if (serviceTypeIds.isEmpty()) {
            return List.of();
        }

        BigDecimal nurseLatitude = BigDecimal.valueOf(nurseLocation.getY());
        BigDecimal nurseLongitude = BigDecimal.valueOf(nurseLocation.getX());

        return serviceRequestRepository.findOpenRequestsForServiceTypes(
                        serviceTypeIds,
                        List.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.SEARCHING, ServiceRequestStatus.NEGOTIATING))
                .stream()
                .filter(request -> request.getLatitude() != null && request.getLongitude() != null)
                .map(request -> toNearbyNurseResponse(request, nurseLatitude, nurseLongitude))
                .filter(request -> request.distanceKm() <= nearbyNursesRadiusKm)
                .sorted(Comparator.comparingDouble(NearbyNurseServiceRequestResponse::distanceKm))
                .toList();
    }

    @Override
    @Transactional
    public void cancelRequest(UUID serviceRequestId, UUID userId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findByIdAndIsDeletedFalse(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + serviceRequestId));

        boolean isOwner = serviceRequest.getProfile() != null
                && serviceRequest.getProfile().getUser() != null
                && serviceRequest.getProfile().getUser().getId().equals(userId);
        boolean isAssignedNurse = serviceRequest.getNurse() != null
                && serviceRequest.getNurse().getUser() != null
                && serviceRequest.getNurse().getUser().getId().equals(userId);
        if (!isOwner && !isAssignedNurse) {
            throw new ResourceNotFoundException("Service request not found: " + serviceRequestId);
        }

        if (isOwner && serviceRequest.getNurse() != null && serviceRequest.getNurse().getUser() != null) {
            UUID nurseUserId = serviceRequest.getNurse().getUser().getId();
            notificationService.create(new NotificationRequest(
                    nurseUserId,
                    "Request Cancelled",
                    "The service request has been cancelled.",
                    NotificationType.BOOKING,
                    "SERVICE_REQUEST",
                    serviceRequestId));
        }
        if (isOwner && serviceRequest.getNurse() == null) {
            nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(serviceRequestId)
                    .stream()
                    .filter(offer -> offer.getStatus() == NurseOfferStatus.PENDING)
                    .filter(offer -> offer.getNurse() != null && offer.getNurse().getUser() != null)
                    .map(offer -> offer.getNurse().getUser().getId())
                    .distinct()
                    .forEach(nurseUserId -> notificationService.create(new NotificationRequest(
                            nurseUserId,
                            "Request Cancelled",
                            "The service request has been cancelled.",
                            NotificationType.BOOKING,
                            "SERVICE_REQUEST",
                            serviceRequestId)));
        }
        if (isAssignedNurse && serviceRequest.getProfile() != null
                && serviceRequest.getProfile().getUser() != null) {
            UUID patientUserId = serviceRequest.getProfile().getUser().getId();
            notificationService.create(new NotificationRequest(
                    patientUserId,
                    "Request Cancelled",
                    "The service request has been cancelled.",
                    NotificationType.BOOKING,
                    "SERVICE_REQUEST",
                    serviceRequestId));
        }


        Set<ServiceRequestStatus> cancellableStatuses = Set.of(
                ServiceRequestStatus.PENDING,
                ServiceRequestStatus.SEARCHING,
                ServiceRequestStatus.NEGOTIATING,
                ServiceRequestStatus.BOOKING,
                ServiceRequestStatus.ACCEPTED);
        if (!cancellableStatuses.contains(serviceRequest.getStatus())) {
            throw new BadRequestException("This service request cannot be cancelled");
        }

        serviceRequest.setStatus(ServiceRequestStatus.CANCELLED);
        nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(serviceRequestId)
                .stream()
                .filter(offer -> offer.getStatus() == NurseOfferStatus.PENDING)
                .forEach(offer -> offer.setStatus(NurseOfferStatus.REJECTED));

        tokenService.delete(visitCodeKey(serviceRequestId));
        tokenService.delete(visitCodeAttemptsKey(serviceRequestId));

        afterCommitExecutor.execute(() ->
                messagingTemplate.convertAndSend(RESERVATION_TOPIC_PREFIX + serviceRequestId,
                        new ReservationEvent("REQUEST_CANCELLED", serviceRequestId, Map.of())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyNurse> getNearbyNursesForRequest(UUID serviceRequestId, UUID userId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findByIdAndIsDeletedFalse(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + serviceRequestId));

        if (!serviceRequest.getProfile().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Service request not found: " + serviceRequestId);
        }

        Set<ServiceRequestStatus> openStatuses = Set.of(
                ServiceRequestStatus.PENDING,
                ServiceRequestStatus.SEARCHING,
                ServiceRequestStatus.NEGOTIATING);
        if (!openStatuses.contains(serviceRequest.getStatus())) {
            throw new BadRequestException("This service request is not open for matching anymore");
        }

        return findNearbyNursesFor(
                serviceRequest.getServiceType(),
                serviceRequest.getLatitude(),
                serviceRequest.getLongitude());
    }

    @Override
    @Transactional
    public void cancelOpenRequestsForUser(UUID userId) {
        List<ServiceRequest> openRequests = serviceRequestRepository
                .findByProfile_User_IdAndIsDeletedFalseAndStatusInAndNurseNullOrderByCreatedAtDesc(
                        userId, OPEN_UNASSIGNED_STATUSES);
        for (ServiceRequest openRequest : openRequests) {
            cancelRequest(openRequest.getId(), userId);
        }
    }

    @Override
    @Transactional
    public VisitCodeResponse generateVisitCode(UUID serviceRequestId, UUID userId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findByIdAndIsDeletedFalse(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + serviceRequestId));

        if (!serviceRequest.getProfile().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Service request not found: " + serviceRequestId);
        }

        if (serviceRequest.getStatus() != ServiceRequestStatus.ACCEPTED) {
            throw new BadRequestException("Visit code can only be generated for an accepted reservation");
        }

        Duration ttl = Duration.ofHours(visitCodeTtlHours);
        String codeKey = visitCodeKey(serviceRequestId);

        String existing = tokenService.get(codeKey);
        if (existing != null) {
            return new VisitCodeResponse(serviceRequestId, existing, Instant.now().plus(ttl));
        }

        String code = generateCode();
        tokenService.set(codeKey, code, ttl);
        tokenService.delete(visitCodeAttemptsKey(serviceRequestId));

        notificationService.create(new NotificationRequest(
                userId,
                "Visit Code Ready",
                "Your visit code is ready — show the QR to your nurse when they arrive.",
                NotificationType.BOOKING,
                "SERVICE_REQUEST",
                serviceRequestId));

        return new VisitCodeResponse(serviceRequestId, code, Instant.now().plus(ttl));
    }

    @Override
    @Transactional
    public void completeRequest(UUID serviceRequestId, String visitCode, UUID userId) {
        Nurse nurse = nurseRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse profile not found"));

        ServiceRequest serviceRequest = serviceRequestRepository.findWithDetailsById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + serviceRequestId));

        if (serviceRequest.getNurse() == null || !serviceRequest.getNurse().getId().equals(nurse.getId())) {
            throw new ForbiddenException("Only the assigned nurse can complete this reservation");
        }

        if (serviceRequest.getStatus() != ServiceRequestStatus.ACCEPTED) {
            throw new BadRequestException("This service request cannot be completed");
        }

        Duration ttl = Duration.ofHours(visitCodeTtlHours);
        String codeKey = visitCodeKey(serviceRequestId);
        String storedCode = tokenService.get(codeKey);
        if (storedCode == null) {
            throw new BadRequestException("Visit code is expired or was never generated");
        }

        if (!constantTimeEquals(storedCode, visitCode)) {
            String attemptsKey = visitCodeAttemptsKey(serviceRequestId);
            Long attempts = tokenService.increment(attemptsKey);
            if (attempts != null && attempts == 1) {
                tokenService.expire(attemptsKey, ttl);
            }
            if (attempts != null && attempts >= visitCodeMaxAttempts) {
                tokenService.delete(codeKey);
                tokenService.delete(attemptsKey);
                throw new BadRequestException("Too many failed attempts. Visit code has been invalidated.");
            }
            throw new BadRequestException("Invalid visit code");
        }

        tokenService.delete(codeKey);
        tokenService.delete(visitCodeAttemptsKey(serviceRequestId));

        serviceRequest.setStatus(ServiceRequestStatus.COMPLETED);
        serviceRequestRepository.save(serviceRequest);

        afterCommitExecutor.execute(() ->
                messagingTemplate.convertAndSend(RESERVATION_TOPIC_PREFIX + serviceRequestId,
                        new ReservationEvent("COMPLETED", serviceRequestId, null)));

        notificationService.create(new NotificationRequest(
                serviceRequest.getProfile().getUser().getId(),
                "Reservation Completed",
                "Your reservation has been completed. Thank you!",
                NotificationType.BOOKING,
                "SERVICE_REQUEST",
                serviceRequestId));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestDetailsResponse getDetails(UUID serviceRequestId, UUID userId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findWithDetailsById(serviceRequestId)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + serviceRequestId));

        if (!serviceRequestRepository.isParticipant(serviceRequestId, userId)) {
            throw new ResourceNotFoundException("Service request not found: " + serviceRequestId);
        }

        return toDetails(serviceRequest, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestDetailsResponse getCurrentVisit(UUID userId) {
        Optional<ServiceRequest> current = nurseRepository.findByUser_Id(userId)
                .flatMap(nurse -> serviceRequestRepository
                        .findFirstByNurse_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(nurse.getId(), VISIT_STATUSES));
        if (current.isEmpty()) {
            current = serviceRequestRepository
                    .findFirstByProfile_User_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(userId, VISIT_STATUSES);
        }
        ServiceRequest serviceRequest = current
                .orElseThrow(() -> new ResourceNotFoundException("No current visit found"));
        return toDetails(serviceRequest, userId);
    }

    private ServiceRequestDetailsResponse toDetails(ServiceRequest serviceRequest, UUID userId) {
        List<NurseOfferResponse> offers = nurseOfferRepository
                .findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(serviceRequest.getId())
                .stream()
                .map(this::toOfferResponseWithDistance)
                .toList();

        Profile profile = serviceRequest.getProfile();
        User profileUser = profile.getUser();
        ServiceRequestDetailsResponse.ProfileSummary profileSummary = new ServiceRequestDetailsResponse.ProfileSummary(
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profileUser.getPhoneNumber(),
                ProfileImageUtil.resolveProfileImageUrl(profile));

        ServiceType serviceType = serviceRequest.getServiceType();
        ServiceRequestDetailsResponse.ServiceTypeSummary serviceTypeSummary = serviceType == null
                ? null
                : new ServiceRequestDetailsResponse.ServiceTypeSummary(
                        serviceType.getId(),
                        serviceType.getName(),
                        serviceType.getBasePrice(),
                        serviceType.getEstimatedDurationMinutes());

        Nurse nurse = serviceRequest.getNurse();
        ServiceRequestDetailsResponse.NurseSummary nurseSummary = null;
        Double distanceKm = null;
        if (nurse != null) {
            User nurseUser = nurse.getUser();
            nurseSummary = new ServiceRequestDetailsResponse.NurseSummary(
                    nurse.getId(),
                    nurseUser.getFirstName(),
                    nurseUser.getLastName(),
                    nurseUser.getPhoneNumber(),
                    nurseUser.getProfileImageUrl(),
                    nurse.getRatingAvg(),
                    nurse.getTotalReviews());
            distanceKm = computeDistanceKm(serviceRequest, nurse);
        }

        return new ServiceRequestDetailsResponse(
                serviceRequest.getId(),
                serviceTypeSummary,
                profileSummary,
                nurseSummary,
                serviceRequest.getServiceDescription(),
                serviceRequest.getPreferredDate(),
                serviceRequest.getPreferredTime(),
                serviceRequest.getDurationMinutes(),
                serviceRequest.getStatus(),
                serviceRequest.getLatitude(),
                serviceRequest.getLongitude(),
                distanceKm,
                serviceRequest.getCreatedAt(),
                serviceRequest.getUpdatedAt(),
                offers);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestNursePreviewResponse getNursePreview(UUID serviceRequestId, UUID userId) {
        Nurse nurse = requireApprovedNurse(userId);

        ServiceRequest request = serviceRequestRepository.findWithDetailsById(serviceRequestId)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + serviceRequestId));

        Set<ServiceRequestStatus> openStatuses = Set.of(
                ServiceRequestStatus.PENDING,
                ServiceRequestStatus.SEARCHING,
                ServiceRequestStatus.NEGOTIATING);
        if (request.getNurse() != null || !openStatuses.contains(request.getStatus())) {
            throw new ResourceNotFoundException("Service request not found: " + serviceRequestId);
        }

        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BadRequestException("Service request location is unavailable");
        }
        if (!providesService(nurse, request.getServiceType())) {
            throw new ForbiddenException("Nurse does not provide the requested service");
        }

        Point nurseLocation = webSocketPresenceService.getAvailableLocation(userId.toString())
                .orElseThrow(() -> new BadRequestException("Nurse location is unavailable"));

        double distanceKm = HaversineUtil.distanceKm(
                BigDecimal.valueOf(nurseLocation.getY()),
                BigDecimal.valueOf(nurseLocation.getX()),
                request.getLatitude(),
                request.getLongitude());

        if (distanceKm > nearbyNursesRadiusKm) {
            throw new ForbiddenException("Nurse is outside the matching radius for this service request");
        }

        ServiceType serviceType = request.getServiceType();
        PatientMedicalSummary summary = patientMedicalSummaryAssembler.build(request.getProfile(), false);

        return new ServiceRequestNursePreviewResponse(
                request.getId(),
                serviceType.getId(),
                serviceType.getName(),
                request.getServiceDescription(),
                request.getPreferredDate(),
                request.getPreferredTime(),
                request.getStatus(),
                priceEstimator.estimate(serviceType.getBasePrice(), distanceKm),
                request.getCreatedAt(),
                summary);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestNurseProfileResponse getAssignedNurseProfile(UUID serviceRequestId, UUID userId) {
        ServiceRequest request = serviceRequestRepository.findWithDetailsById(serviceRequestId)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + serviceRequestId));

        if (request.getNurse() == null || !request.getNurse().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Service request not found: " + serviceRequestId);
        }

        Set<ServiceRequestStatus> assignedStatuses = Set.of(
                ServiceRequestStatus.BOOKING,
                ServiceRequestStatus.ACCEPTED,
                ServiceRequestStatus.IN_PROGRESS,
                ServiceRequestStatus.COMPLETED);
        if (!assignedStatuses.contains(request.getStatus())) {
            throw new BadRequestException("Service request is not assigned yet");
        }

        Profile profile = request.getProfile();
        ServiceType serviceType = request.getServiceType();
        PatientMedicalSummary summary = patientMedicalSummaryAssembler.build(profile, true);

        String patientPhoneNumber = profile.getUser() == null ? null : profile.getUser().getPhoneNumber();

        ServiceRequestNurseProfileResponse.AddressSummary addressSummary = addressRepository
                .findByProfileId(profile.getId())
                .map(a -> new ServiceRequestNurseProfileResponse.AddressSummary(
                        a.getCountry(),
                        a.getCity(),
                        a.getArea(),
                        a.getStreet(),
                        a.getBuildingNumber(),
                        a.getApartmentNumber()))
                .orElse(null);

        double distanceKm = webSocketPresenceService
                .getAvailableLocation(userId.toString())
                .map(location -> HaversineUtil.distanceKm(
                        BigDecimal.valueOf(location.getY()),
                        BigDecimal.valueOf(location.getX()),
                        request.getLatitude(),
                        request.getLongitude()))
                .orElse(0.0);

        return new ServiceRequestNurseProfileResponse(
                request.getId(),
                serviceType.getId(),
                serviceType.getName(),
                request.getServiceDescription(),
                request.getPreferredDate(),
                request.getPreferredTime(),
                request.getStatus(),
                priceEstimator.estimate(serviceType.getBasePrice(), distanceKm),
                request.getCreatedAt(),
                summary,
                patientPhoneNumber,
                addressSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceRequestHistoryResponse> listConfirmedHistory(UUID userId) {
        List<ServiceRequestStatus> confirmedStatuses = List.of(
                ServiceRequestStatus.ACCEPTED,
                ServiceRequestStatus.IN_PROGRESS,
                ServiceRequestStatus.COMPLETED);

        return serviceRequestRepository
                .findByProfile_User_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(userId, confirmedStatuses)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private ServiceRequestHistoryResponse toHistoryResponse(ServiceRequest s) {
        ServiceType serviceType = s.getServiceType();
        Nurse nurse = s.getNurse();
        String nurseName = null;
        if (nurse != null && nurse.getUser() != null) {
            User nurseUser = nurse.getUser();
            String first = nurseUser.getFirstName() == null ? "" : nurseUser.getFirstName();
            String last = nurseUser.getLastName() == null ? "" : nurseUser.getLastName();
            String combined = (first + " " + last).trim();
            nurseName = combined.isEmpty() ? null : combined;
        }

        Double distanceKm = nurse != null ? computeDistanceKm(s, nurse) : null;

        return new ServiceRequestHistoryResponse(
                s.getId(),
                serviceType == null ? null : serviceType.getId(),
                serviceType == null ? null : serviceType.getName(),
                s.getServiceDescription(),
                s.getPreferredDate(),
                s.getPreferredTime(),
                s.getStatus(),
                nurse == null ? null : nurse.getId(),
                nurseName,
                nurse != null && nurse.getUser() != null ? nurse.getUser().getProfileImageUrl() : null,
                distanceKm,
                serviceType == null ? null : serviceType.getEstimatedDurationMinutes(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }

    private NurseOfferResponse toOfferResponseWithDistance(NurseOffer offer) {
        ServiceRequest serviceRequest = offer.getServiceRequest();
        ServiceType serviceType = serviceRequest.getServiceType();
        NurseOfferResponse base = nurseOfferMapper.toResponse(offer);
        return new NurseOfferResponse(
                base.id(),
                base.serviceRequestId(),
                base.nurse(),
                base.proposedPrice(),
                base.proposedDate(),
                base.proposedTime(),
                base.message(),
                base.status(),
                computeDistanceKm(serviceRequest, offer.getNurse()),
                serviceType == null ? null : serviceType.getName(),
                serviceType == null ? null : serviceType.getEstimatedDurationMinutes(),
                base.createdAt(),
                base.updatedAt());
    }

    private Double computeDistanceKm(ServiceRequest serviceRequest, Nurse nurse) {
        if (serviceRequest.getLatitude() == null || serviceRequest.getLongitude() == null || nurse == null) {
            return null;
        }
        return webSocketPresenceService
                .getAvailableLocation(nurse.getUser().getId().toString())
                .map(location -> HaversineUtil.distanceKm(
                        serviceRequest.getLatitude(),
                        serviceRequest.getLongitude(),
                        BigDecimal.valueOf(location.getY()),
                        BigDecimal.valueOf(location.getX())))
                .orElse(null);
    }

    private String visitCodeKey(UUID serviceRequestId) {
        return VISIT_CODE_PREFIX + serviceRequestId;
    }

    private String visitCodeAttemptsKey(UUID serviceRequestId) {
        return VISIT_CODE_ATTEMPTS_PREFIX + serviceRequestId;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private Nurse requireApprovedNurse(UUID userId) {
        Nurse nurse = nurseRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse profile not found"));
        if (nurse.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new BadRequestException("Nurse is not eligible to receive service requests");
        }
        return nurse;
    }

    private boolean providesService(Nurse nurse, ServiceType serviceType) {
        return nurseServiceRepository
                .findByNurse_IdAndServiceType_Id(nurse.getId(), serviceType.getId())
                .map(nurseService -> Boolean.TRUE.equals(nurseService.getIsActive()))
                .orElse(false);
    }

    private List<NearbyNurse> findNearbyNursesFor(ServiceType serviceType, BigDecimal latitude, BigDecimal longitude) {
        Set<UUID> nursesForRequiredService = nurseServiceRepository
                .findByServiceType_IdAndIsActiveTrue(serviceType.getId())
                .stream()
                .map(NurseService::getNurse)
                .map(Nurse::getId)
                .collect(Collectors.toSet());

        List<NurseLocation> nurseLocations = nurseLocationProvider.getNurseLocations();

        log.info("nurseLocations: {}" , nurseLocations);

        List<NearbyNurse> nearbyNurses = nearbyNurseMatcher.findNearbyNurse(
                nurseLocations,
                latitude,
                longitude,
                nursesForRequiredService);

        log.info("nearbyNurses: {}" , nearbyNurses);

        return nearbyNurses;
    }

    private NearbyNurseServiceRequestResponse toNearbyNurseResponse(ServiceRequest request, BigDecimal nurseLatitude, BigDecimal nurseLongitude) {
        double distanceKm = HaversineUtil.distanceKm(
                nurseLatitude, nurseLongitude, request.getLatitude(), request.getLongitude());
        Profile profile = request.getProfile();

        return new NearbyNurseServiceRequestResponse(
                request.getId(),
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                ProfileImageUtil.resolveProfileImageUrl(profile),
                request.getServiceType().getId(),
                request.getServiceType().getName(),
                request.getServiceDescription(),
                request.getPreferredDate(),
                request.getPreferredTime(),
                request.getStatus(),
                request.getLatitude(),
                request.getLongitude(),
                distanceKm,
                priceEstimator.estimate(request.getServiceType().getBasePrice(), distanceKm),
                request.getServiceType().getEstimatedDurationMinutes(),
                request.getCreatedAt());
    }
}
