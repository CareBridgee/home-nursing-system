package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.nurse.NearbyNurse;
import iti.jets.java.homenursing.dto.servicerequest.CompleteServiceRequestRequest;
import iti.jets.java.homenursing.dto.servicerequest.NearbyNurseServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestRequest;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.VisitCodeResponse;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NurseRepository nurseRepository;
    private final ServiceTypeRepository serviceTypeRepository;

    @PostMapping
    public ResponseEntity<NearbyServiceRequestResponse> createRequest(@Valid @RequestBody NearbyServiceRequestRequest request) {
        NearbyServiceRequestResponse response = serviceRequestService.createRequest(request);

        pushToNearbyNurses(response);

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/service-requests/" + response.serviceRequestId()))
                .body(response);
    }

    private void pushToNearbyNurses(NearbyServiceRequestResponse response) {
        if (response.nearbyNurses() == null) return;

        String serviceName = serviceTypeRepository.findById(response.serviceTypeId())
                .map(ServiceType::getName)
                .orElse(null);

        for (NearbyNurse nearby : response.nearbyNurses()) {
            Nurse nurse = nurseRepository.findWithUserById(nearby.nurseId()).orElse(null);
            if (nurse == null) continue;

            NearbyNurseServiceRequestResponse pushPayload = new NearbyNurseServiceRequestResponse(
                    response.serviceRequestId(),
                    response.profileId(),
                    response.serviceTypeId(),
                    serviceName,
                    null,
                    null,
                    null,
                    response.status(),
                    response.latitude(),
                    response.longitude(),
                    nearby.distanceKm(),
                    null,
                    response.createdAt());

            String nurseUserId = nurse.getUser().getId().toString();
            messagingTemplate.convertAndSendToUser(nurseUserId, "/queue/nearby-request", pushPayload);
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyNurseServiceRequestResponse>> listNearbyForNurse() {
        return ResponseEntity.ok(serviceRequestService.listNearbyForNurse(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{serviceRequestId}/nearby-nurses")
    public ResponseEntity<List<NearbyNurse>> getNearbyNursesForRequest(@PathVariable UUID serviceRequestId) {
        return ResponseEntity.ok(serviceRequestService.getNearbyNursesForRequest(serviceRequestId, SecurityUtils.currentUserId()));
    }

    @PatchMapping("/{serviceRequestId}/cancel")
    public ResponseEntity<Void> cancelRequest(@PathVariable UUID serviceRequestId) {
        serviceRequestService.cancelRequest(serviceRequestId, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{serviceRequestId}/visit-code")
    public ResponseEntity<VisitCodeResponse> generateVisitCode(@PathVariable UUID serviceRequestId) {
        VisitCodeResponse response = serviceRequestService.generateVisitCode(serviceRequestId, SecurityUtils.currentUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{serviceRequestId}/complete")
    public ResponseEntity<Void> completeRequest(@PathVariable UUID serviceRequestId,
                                                @Valid @RequestBody CompleteServiceRequestRequest request) {
        serviceRequestService.completeRequest(serviceRequestId, request.visitCode(), SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
