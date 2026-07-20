package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.ProfileAllergyRequest;
import iti.jets.java.homenursing.dto.ProfileAllergyResponse;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.ProfileAllergyService;
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
public class ProfileAllergyController {

    private final ProfileAllergyService profileAllergyService;

    public ProfileAllergyController(ProfileAllergyService profileAllergyService) {
        this.profileAllergyService = profileAllergyService;
    }

    @GetMapping("/profiles/{profileId}/allergies")
    public ResponseEntity<List<ProfileAllergyResponse>> listByProfile(@PathVariable UUID profileId) {
        return ResponseEntity.ok(profileAllergyService.listByProfile(profileId, SecurityUtils.currentUserId()));
    }

    @PostMapping("/profiles/{profileId}/allergies")
    public ResponseEntity<ProfileAllergyResponse> addToProfile(
            @PathVariable UUID profileId,
            @Valid @RequestBody ProfileAllergyRequest request) {
        ProfileAllergyResponse response = profileAllergyService.addToProfile(profileId, SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/profiles/" + profileId + "/allergies/" + response.allergyId()))
                .body(response);
    }

    @DeleteMapping("/profiles/{profileId}/allergies/{allergyId}")
    public ResponseEntity<Void> removeFromProfile(
            @PathVariable UUID profileId,
            @PathVariable UUID allergyId) {
        profileAllergyService.removeFromProfile(profileId, SecurityUtils.currentUserId(), allergyId);
        return ResponseEntity.noContent().build();
    }
}
