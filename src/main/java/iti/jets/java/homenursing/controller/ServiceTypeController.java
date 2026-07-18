package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.ServiceTypeResponse;
import iti.jets.java.homenursing.service.ServiceTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
