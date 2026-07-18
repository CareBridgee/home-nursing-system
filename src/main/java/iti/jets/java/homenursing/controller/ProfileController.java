package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.ProfileRequest;
import iti.jets.java.homenursing.dto.ProfileResponse;
import iti.jets.java.homenursing.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<List<ProfileResponse>> listProfiles() {
        UUID userId = currentUserId();
        return ResponseEntity.ok(profileService.listProfiles(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable UUID id) {
        UUID userId = currentUserId();
        return ResponseEntity.ok(profileService.getOwnedProfile(id, userId));
    }

    @GetMapping("/default")
    public ResponseEntity<ProfileResponse> getDefaultProfile() {
        UUID userId = currentUserId();
        return ResponseEntity.ok(profileService.getDefaultProfile(userId));
    }

    @PostMapping
    public ResponseEntity<ProfileResponse> createFamilyProfile(@Valid @RequestBody ProfileRequest request) {
        UUID userId = currentUserId();
        ProfileResponse response = profileService.createFamilyProfile(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/profiles/" + response.getId()))
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponse> updateProfile(@PathVariable UUID id,
                                                         @Valid @RequestBody ProfileRequest request) {
        UUID userId = currentUserId();
        return ResponseEntity.ok(profileService.updateProfile(id, userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFamilyProfile(@PathVariable UUID id) {
        UUID userId = currentUserId();
        profileService.deleteFamilyProfile(id, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}
