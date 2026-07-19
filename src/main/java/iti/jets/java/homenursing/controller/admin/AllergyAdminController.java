package iti.jets.java.homenursing.controller.admin;

import iti.jets.java.homenursing.dto.AllergyRequest;
import iti.jets.java.homenursing.dto.AllergyResponse;
import iti.jets.java.homenursing.service.AllergyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog/allergies")
public class AllergyAdminController {

    private final AllergyService allergyService;

    public AllergyAdminController(AllergyService allergyService) {
        this.allergyService = allergyService;
    }

    @PostMapping
    public ResponseEntity<AllergyResponse> create(@Valid @RequestBody AllergyRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        AllergyResponse response = allergyService.create(request);
        URI location = uriBuilder.path("/api/v1/admin/catalog/allergies/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
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
