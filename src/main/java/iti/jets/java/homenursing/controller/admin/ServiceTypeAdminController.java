package iti.jets.java.homenursing.controller.admin;

import iti.jets.java.homenursing.dto.ServiceTypeRequest;
import iti.jets.java.homenursing.dto.ServiceTypeResponse;
import iti.jets.java.homenursing.service.ServiceTypeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog/service-types")
public class ServiceTypeAdminController {

    private final ServiceTypeService serviceTypeService;

    public ServiceTypeAdminController(ServiceTypeService serviceTypeService) {
        this.serviceTypeService = serviceTypeService;
    }

    @PostMapping
    public ResponseEntity<ServiceTypeResponse> create(@Valid @RequestBody ServiceTypeRequest request,
                                                      UriComponentsBuilder uriBuilder) {
        ServiceTypeResponse response = serviceTypeService.create(request);
        URI location = uriBuilder.path("/api/v1/admin/catalog/service-types/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
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
