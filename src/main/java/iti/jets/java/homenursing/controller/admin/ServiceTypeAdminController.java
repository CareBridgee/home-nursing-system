package iti.jets.java.homenursing.controller.admin;

import iti.jets.java.homenursing.dto.ServiceTypeRequest;
import iti.jets.java.homenursing.dto.ServiceTypeResponse;
import iti.jets.java.homenursing.service.ServiceTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog/service-types")
@RequiredArgsConstructor
public class ServiceTypeAdminController {

    private final ServiceTypeService serviceTypeService;

    @PostMapping
    public ResponseEntity<ServiceTypeResponse> create(@Valid @RequestBody ServiceTypeRequest request) {
        ServiceTypeResponse created = serviceTypeService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/catalog/service-types/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceTypeResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody ServiceTypeRequest request) {
        return ResponseEntity.ok(serviceTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        serviceTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
