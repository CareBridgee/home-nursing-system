package iti.jets.java.homenursing.controller.admin;

import iti.jets.java.homenursing.dto.MedicalConditionRequest;
import iti.jets.java.homenursing.dto.MedicalConditionResponse;
import iti.jets.java.homenursing.service.MedicalConditionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog/medical-conditions")
@RequiredArgsConstructor
public class MedicalConditionAdminController {

    private final MedicalConditionService medicalConditionService;

    @PostMapping
    public ResponseEntity<MedicalConditionResponse> create(@Valid @RequestBody MedicalConditionRequest request) {
        MedicalConditionResponse created = medicalConditionService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/catalog/medical-conditions/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalConditionResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody MedicalConditionRequest request) {
        return ResponseEntity.ok(medicalConditionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        medicalConditionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
