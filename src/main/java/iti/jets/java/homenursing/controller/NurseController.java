package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.nurse.NurseRegistrationRequest;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.nurse.NurseServiceRequest;
import iti.jets.java.homenursing.dto.nurse.NurseServiceResponse;
import iti.jets.java.homenursing.dto.nurse.NurseUpdateRequest;
import iti.jets.java.homenursing.dto.nurse.UpdateServicePriceRequest;
import iti.jets.java.homenursing.service.NurseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nurses")
public class NurseController {

    private final NurseService nurseService;

    public NurseController(NurseService nurseService) {
        this.nurseService = nurseService;
    }

    @PostMapping("/register")
    public ResponseEntity<NurseResponse> register(@Valid @RequestBody NurseRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nurseService.register(request));
    }

    @PutMapping("/{nurseId}")
    public ResponseEntity<NurseResponse> updateProfile(@PathVariable UUID nurseId, @RequestBody NurseUpdateRequest request) {
        return ResponseEntity.ok(nurseService.updateProfile(nurseId, request));
    }

    @GetMapping("/{nurseId}")
    public ResponseEntity<NurseResponse> getProfile(@PathVariable UUID nurseId) {
        return ResponseEntity.ok(nurseService.getProfile(nurseId));
    }

    @PostMapping("/{nurseId}/services")
    public ResponseEntity<NurseServiceResponse> addService(@PathVariable UUID nurseId, @Valid @RequestBody NurseServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nurseService.addService(nurseId, request));
    }

    @PatchMapping("/{nurseId}/services/{serviceTypeId}/price")
    public ResponseEntity<NurseServiceResponse> updateServicePrice(@PathVariable UUID nurseId,
                                                                   @PathVariable UUID serviceTypeId,
                                                                   @Valid @RequestBody UpdateServicePriceRequest request) {
        return ResponseEntity.ok(nurseService.updateServicePrice(nurseId, serviceTypeId, request));
    }
}
