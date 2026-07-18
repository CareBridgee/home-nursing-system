package iti.jets.java.homenursing.controller.admin;

import iti.jets.java.homenursing.dto.AllergyRequest;
import iti.jets.java.homenursing.dto.AllergyResponse;
import iti.jets.java.homenursing.service.AllergyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog/allergies")
@RequiredArgsConstructor
public class AllergyAdminController {

    private final AllergyService allergyService;

    @PostMapping
    public ResponseEntity<AllergyResponse> create(@Valid @RequestBody AllergyRequest request) {
        AllergyResponse created = allergyService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/catalog/allergies/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AllergyResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody AllergyRequest request) {
        return ResponseEntity.ok(allergyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        allergyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
