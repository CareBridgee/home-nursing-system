package iti.jets.java.homenursing.controller.admin;

import iti.jets.java.homenursing.dto.MedicationRequest;
import iti.jets.java.homenursing.dto.MedicationResponse;
import iti.jets.java.homenursing.service.MedicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog/medications")
@RequiredArgsConstructor
public class MedicationAdminController {

    private final MedicationService medicationService;

    @PostMapping
    public ResponseEntity<MedicationResponse> create(@Valid @RequestBody MedicationRequest request) {
        MedicationResponse created = medicationService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/catalog/medications/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicationResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody MedicationRequest request) {
        return ResponseEntity.ok(medicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        medicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
