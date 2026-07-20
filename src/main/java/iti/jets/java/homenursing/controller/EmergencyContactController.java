package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.EmergencyContactRequest;
import iti.jets.java.homenursing.dto.EmergencyContactResponse;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.EmergencyContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    public EmergencyContactController(EmergencyContactService emergencyContactService) {
        this.emergencyContactService = emergencyContactService;
    }

    @GetMapping("/profiles/{profileId}/emergency-contacts")
    public ResponseEntity<List<EmergencyContactResponse>> listByProfile(@PathVariable UUID profileId) {
        return ResponseEntity.ok(emergencyContactService.listByProfile(profileId, SecurityUtils.currentUserId()));
    }

    @GetMapping("/emergency-contacts/{id}")
    public ResponseEntity<EmergencyContactResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(emergencyContactService.getById(id, SecurityUtils.currentUserId()));
    }

    @PostMapping("/profiles/{profileId}/emergency-contacts")
    public ResponseEntity<EmergencyContactResponse> create(
            @PathVariable UUID profileId,
            @Valid @RequestBody EmergencyContactRequest request) {
        EmergencyContactResponse response = emergencyContactService.create(profileId, SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/emergency-contacts/" + response.getId()))
                .body(response);
    }

    @PutMapping("/emergency-contacts/{id}")
    public ResponseEntity<EmergencyContactResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody EmergencyContactRequest request) {
        return ResponseEntity.ok(emergencyContactService.update(id, SecurityUtils.currentUserId(), request));
    }

    @DeleteMapping("/emergency-contacts/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        emergencyContactService.delete(id, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
