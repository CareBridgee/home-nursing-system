package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.profile.ProfileRequest;
import iti.jets.java.homenursing.dto.profile.ProfileResponse;
import iti.jets.java.homenursing.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ModelAttribute;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> createFamilyProfile(@Valid @ModelAttribute ProfileRequest request) {
        UUID userId = currentUserId();
        ProfileResponse response = profileService.createFamilyProfile(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/profiles/" + response.getId()))
                .body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> updateProfile(@PathVariable UUID id,
                                                         @Valid @ModelAttribute ProfileRequest request) {
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
