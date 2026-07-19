package iti.jets.java.homenursing.controller.admin;

import iti.jets.java.homenursing.dto.MedicalConditionRequest;
import iti.jets.java.homenursing.dto.MedicalConditionResponse;
import iti.jets.java.homenursing.service.MedicalConditionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog/medical-conditions")
public class MedicalConditionAdminController {

    private final MedicalConditionService medicalConditionService;

    public MedicalConditionAdminController(MedicalConditionService medicalConditionService) {
        this.medicalConditionService = medicalConditionService;
    }

    @PostMapping
    public ResponseEntity<MedicalConditionResponse> create(@Valid @RequestBody MedicalConditionRequest request,
                                                           UriComponentsBuilder uriBuilder) {
        MedicalConditionResponse response = medicalConditionService.create(request);
        URI location = uriBuilder.path("/api/v1/admin/catalog/medical-conditions/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
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
