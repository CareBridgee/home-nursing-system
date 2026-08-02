package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.dto.nurse.NearbyNurse;
import iti.jets.java.homenursing.dto.nurse.NurseLocation;
import iti.jets.java.homenursing.dto.reservation.ReservationEvent;
import iti.jets.java.homenursing.dto.servicerequest.NearbyNurseServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestRequest;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.VisitCodeResponse;
import iti.jets.java.homenursing.entity.*;
import iti.jets.java.homenursing.entity.enums.NotificationType;
import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ForbiddenException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.repository.NurseOfferRepository;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.NurseServiceRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.service.NearbyNurseMatcher;
import iti.jets.java.homenursing.service.NurseLocationProvider;
import iti.jets.java.homenursing.service.NotificationService;
import iti.jets.java.homenursing.service.ProfileService;
import iti.jets.java.homenursing.service.ServiceRequestService;
import iti.jets.java.homenursing.service.TokenService;
import iti.jets.java.homenursing.util.HaversineUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
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
    private final ProfileService profileService;
    private final NurseLocationProvider nurseLocationProvider;
    private final NearbyNurseMatcher nearbyNurseMatcher;
    private final WebSocketPresenceService webSocketPresenceService;
    private final TokenService tokenService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${nearby.nurses.radius-km:10}")
    private double nearbyNursesRadiusKm;

    @Value("${nearby.nurses.included-distance-km:5}")
    private double includedDistanceKm;

    @Value("${nearby.nurses.price-per-km:12}")
    private BigDecimal pricePerKm;

    @Value("${visit.code.ttl-hours:24}")
    private long visitCodeTtlHours;

    @Value("${visit.code.max-attempts:5}")
    private int visitCodeMaxAttempts;

    private static final String CODE_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String VISIT_CODE_PREFIX = "visitcode:";
    private static final String VISIT_CODE_ATTEMPTS_PREFIX = "visitcode_attempts:";
    private static final String RESERVATION_TOPIC_PREFIX = "/topic/reservation/";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public NearbyServiceRequestResponse createRequest(NearbyServiceRequestRequest request) {
        Profile profile = profileService.getProfile(request.profileId());
        ServiceType serviceType = serviceTypeRepository.findById(request.serviceTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + request.serviceTypeId()));

        List<NearbyNurse> nearbyNurses = findNearbyNursesFor(serviceType, request.latitude(), request.longitude());

        ServiceRequest serviceRequest = ServiceRequest.builder()
                .profile(profile)
                .serviceType(serviceType)
                .serviceDescription(request.serviceDescription())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .status(ServiceRequestStatus.SEARCHING)
                .preferredDate(request.preferredDate())
                .preferredTime(request.preferredTime())
                .isDeleted(false)
                .build();

        ServiceRequest saved = serviceRequestRepository.save(serviceRequest);

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

        if (!serviceRequest.getProfile().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Service request not found: " + serviceRequestId);
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

        messagingTemplate.convertAndSend(RESERVATION_TOPIC_PREFIX + serviceRequestId,
                new ReservationEvent("COMPLETED", serviceRequestId, null));

        notificationService.create(new NotificationRequest(
                serviceRequest.getProfile().getUser().getId(),
                "Reservation Completed",
                "Your reservation has been completed. Thank you!",
                NotificationType.BOOKING,
                "SERVICE_REQUEST",
                serviceRequestId));
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

        return new NearbyNurseServiceRequestResponse(
                request.getId(),
                request.getProfile().getId(),
                request.getServiceType().getId(),
                request.getServiceType().getName(),
                request.getServiceDescription(),
                request.getPreferredDate(),
                request.getPreferredTime(),
                request.getStatus(),
                request.getLatitude(),
                request.getLongitude(),
                distanceKm,
                calculatePrice(request.getServiceType().getBasePrice(), distanceKm),
                request.getCreatedAt());
    }

    private BigDecimal calculatePrice(BigDecimal basePrice, double distanceKm) {
        double extraDistanceKm = Math.max(0, distanceKm - includedDistanceKm);
        return basePrice
                .add(pricePerKm.multiply(BigDecimal.valueOf(extraDistanceKm)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
