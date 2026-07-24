package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestRequest;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyNurseServiceRequestResponse;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<NearbyServiceRequestResponse> createRequest(@Valid @RequestBody NearbyServiceRequestRequest request) {
        NearbyServiceRequestResponse response = serviceRequestService.createRequest( request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/service-requests/" + response.serviceRequestId()))
                .body(response);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyNurseServiceRequestResponse>> listNearbyForNurse() {
        return ResponseEntity.ok(serviceRequestService.listNearbyForNurse(SecurityUtils.currentUserId()));
    }

    @PatchMapping("/{serviceRequestId}/cancel")
    public ResponseEntity<Void> cancelRequest(@PathVariable UUID serviceRequestId) {
        serviceRequestService.cancelRequest(serviceRequestId, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
