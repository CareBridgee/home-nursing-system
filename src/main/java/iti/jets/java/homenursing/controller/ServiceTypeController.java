package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.catalog.ServiceTypeRequest;
import iti.jets.java.homenursing.dto.catalog.ServiceTypeResponse;
import iti.jets.java.homenursing.service.ServiceTypeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-types")
public class ServiceTypeController {

    private final ServiceTypeService serviceTypeService;

    public ServiceTypeController(ServiceTypeService serviceTypeService) {
        this.serviceTypeService = serviceTypeService;
    }

    @GetMapping
    public List<ServiceTypeResponse> listServiceTypes() {
        return serviceTypeService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceTypeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceTypeService.getById(id));
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceTypeResponse> createServiceType(@ModelAttribute ServiceTypeRequest request) {
        ServiceTypeResponse response = serviceTypeService.create(request);
        return ResponseEntity.ok().body(response);
    }
}
