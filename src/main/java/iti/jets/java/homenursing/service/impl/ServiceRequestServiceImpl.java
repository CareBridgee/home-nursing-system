package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.nurse.NearbyNurse;
import iti.jets.java.homenursing.dto.nurse.NurseLocation;
import iti.jets.java.homenursing.dto.servicerequest.NearbyNurseServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestRequest;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.entity.*;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.NurseServiceRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.service.NearbyNurseMatcher;
import iti.jets.java.homenursing.service.NurseLocationProvider;
import iti.jets.java.homenursing.service.ProfileService;
import iti.jets.java.homenursing.service.ServiceRequestService;
import iti.jets.java.homenursing.util.HaversineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRequestServiceImpl implements ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final NurseRepository nurseRepository;
    private final NurseServiceRepository nurseServiceRepository;
    private final ProfileService profileService;
    private final NurseLocationProvider nurseLocationProvider;
    private final NearbyNurseMatcher nearbyNurseMatcher;
    private final WebSocketPresenceService webSocketPresenceService;

    @Value("${nearby.nurses.radius-km:10}")
    private double nearbyNursesRadiusKm;

    @Override
    @Transactional
    public NearbyServiceRequestResponse createRequest(NearbyServiceRequestRequest request) {
        Profile profile = profileService.getProfile(request.profileId());
        ServiceType serviceType = serviceTypeRepository.findById(request.serviceTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + request.serviceTypeId()));

        // get all nurses ids have this service
        Set<UUID> nursesForRequiredService = nurseServiceRepository
                .findByServiceType_IdAndIsActiveTrue(request.serviceTypeId())
                .stream()
                .map(NurseService::getNurse)
                .map(Nurse::getId)
                .collect(Collectors.toSet());

        if (nursesForRequiredService.isEmpty()) {
            throw new BadRequestException("No nurses currently offer this service");
        }

        // nurse location by socket
        List<NurseLocation> nurseLocations = nurseLocationProvider.getNurseLocations();


        // get all nurses in my area
        List<NearbyNurse> nearbyNurses = nearbyNurseMatcher.findNearbyNurse(
                nurseLocations,
                request.latitude(),
                request.longitude(),
                nursesForRequiredService);

        if (nearbyNurses.isEmpty()) {
            throw new BadRequestException("No nearby nurses found for this service within the given radius");
        }

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

        if (!Boolean.TRUE.equals(nurse.getIsAvailable())
                || nurse.getVerificationStatus() != VerificationStatus.APPROVED) {
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
                request.getCreatedAt());
    }
}
