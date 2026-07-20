package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.ProfileMedicationRequest;
import iti.jets.java.homenursing.dto.ProfileMedicationResponse;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.ProfileMedicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ProfileMedicationController {

    private final ProfileMedicationService profileMedicationService;

    public ProfileMedicationController(ProfileMedicationService profileMedicationService) {
        this.profileMedicationService = profileMedicationService;
    }

    @GetMapping("/profiles/{profileId}/medications")
    public ResponseEntity<List<ProfileMedicationResponse>> listByProfile(@PathVariable UUID profileId) {
        return ResponseEntity.ok(profileMedicationService.listByProfile(profileId, SecurityUtils.currentUserId()));
    }

    @PostMapping("/profiles/{profileId}/medications")
    public ResponseEntity<ProfileMedicationResponse> addToProfile(
            @PathVariable UUID profileId,
            @Valid @RequestBody ProfileMedicationRequest request) {
        ProfileMedicationResponse response = profileMedicationService.addToProfile(profileId, SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/profiles/" + profileId + "/medications/" + response.medicationId()))
                .body(response);
    }

    @DeleteMapping("/profiles/{profileId}/medications/{medicationId}")
    public ResponseEntity<Void> removeFromProfile(
            @PathVariable UUID profileId,
            @PathVariable UUID medicationId) {
        profileMedicationService.removeFromProfile(profileId, SecurityUtils.currentUserId(), medicationId);
        return ResponseEntity.noContent().build();
    }
}
