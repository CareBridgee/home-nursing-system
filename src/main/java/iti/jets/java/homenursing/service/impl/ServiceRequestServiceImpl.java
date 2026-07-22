package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.nurse.NearbyNurse;
import iti.jets.java.homenursing.dto.nurse.NurseLocation;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestRequest;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.entity.*;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.repository.NurseServiceRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.service.NearbyNurseMatcher;
import iti.jets.java.homenursing.service.NurseLocationProvider;
import iti.jets.java.homenursing.service.ProfileService;
import iti.jets.java.homenursing.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRequestServiceImpl implements ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final NurseServiceRepository nurseServiceRepository;
    private final ProfileService profileService;
    private final NurseLocationProvider nurseLocationProvider;
    private final NearbyNurseMatcher nearbyNurseMatcher;



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
}
