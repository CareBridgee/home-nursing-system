package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.MedicalHistoryRequest;
import iti.jets.java.homenursing.dto.MedicalHistoryResponse;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.MedicalHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MedicalHistoryController {

    private final MedicalHistoryService medicalHistoryService;

    public MedicalHistoryController(MedicalHistoryService medicalHistoryService) {
        this.medicalHistoryService = medicalHistoryService;
    }

    @GetMapping("/profiles/{profileId}/medical-history")
    public ResponseEntity<List<MedicalHistoryResponse>> listByProfile(@PathVariable UUID profileId) {
        return ResponseEntity.ok(medicalHistoryService.listByProfile(profileId, SecurityUtils.currentUserId()));
    }

    @GetMapping("/medical-history/{id}")
    public ResponseEntity<MedicalHistoryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(medicalHistoryService.getById(id, SecurityUtils.currentUserId()));
    }

    @PostMapping("/profiles/{profileId}/medical-history")
    public ResponseEntity<MedicalHistoryResponse> create(
            @PathVariable UUID profileId,
            @Valid @RequestBody MedicalHistoryRequest request) {
        MedicalHistoryResponse response = medicalHistoryService.create(profileId, SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/medical-history/" + response.getId()))
                .body(response);
    }

    @PutMapping("/medical-history/{id}")
    public ResponseEntity<MedicalHistoryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody MedicalHistoryRequest request) {
        return ResponseEntity.ok(medicalHistoryService.update(id, SecurityUtils.currentUserId(), request));
    }

    @DeleteMapping("/medical-history/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        medicalHistoryService.delete(id, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
