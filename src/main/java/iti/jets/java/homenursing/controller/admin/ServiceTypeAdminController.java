package iti.jets.java.homenursing.controller.admin;

import iti.jets.java.homenursing.dto.catalog.ServiceTypeRequest;
import iti.jets.java.homenursing.dto.catalog.ServiceTypeResponse;
import iti.jets.java.homenursing.service.ServiceTypeService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceTypeResponse> create(@Valid @ModelAttribute ServiceTypeRequest request,
                                                      UriComponentsBuilder uriBuilder) {
        ServiceTypeResponse response = serviceTypeService.create(request);
        URI location = uriBuilder.path("/api/v1/admin/catalog/service-types/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceTypeResponse> update(@PathVariable UUID id,
                                                      @Valid @ModelAttribute ServiceTypeRequest request) {
        return ResponseEntity.ok(serviceTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        serviceTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
