package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.ProfileMedicalConditionRequest;
import iti.jets.java.homenursing.dto.ProfileMedicalConditionResponse;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.ProfileMedicalConditionService;
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
public class ProfileMedicalConditionController {

    private final ProfileMedicalConditionService profileMedicalConditionService;

    public ProfileMedicalConditionController(ProfileMedicalConditionService profileMedicalConditionService) {
        this.profileMedicalConditionService = profileMedicalConditionService;
    }

    @GetMapping("/profiles/{profileId}/medical-conditions")
    public ResponseEntity<List<ProfileMedicalConditionResponse>> listByProfile(@PathVariable UUID profileId) {
        return ResponseEntity.ok(profileMedicalConditionService.listByProfile(profileId, SecurityUtils.currentUserId()));
    }

    @PostMapping("/profiles/{profileId}/medical-conditions")
    public ResponseEntity<ProfileMedicalConditionResponse> addToProfile(
            @PathVariable UUID profileId,
            @Valid @RequestBody ProfileMedicalConditionRequest request) {
        ProfileMedicalConditionResponse response = profileMedicalConditionService.addToProfile(profileId, SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/profiles/" + profileId + "/medical-conditions/" + response.medicalConditionId()))
                .body(response);
    }

    @DeleteMapping("/profiles/{profileId}/medical-conditions/{medicalConditionId}")
    public ResponseEntity<Void> removeFromProfile(
            @PathVariable UUID profileId,
            @PathVariable UUID medicalConditionId) {
        profileMedicalConditionService.removeFromProfile(profileId, SecurityUtils.currentUserId(), medicalConditionId);
        return ResponseEntity.noContent().build();
    }
}
