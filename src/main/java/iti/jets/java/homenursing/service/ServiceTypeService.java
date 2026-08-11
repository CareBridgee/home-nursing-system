package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.catalog.ServiceTypeRequest;
import iti.jets.java.homenursing.dto.catalog.ServiceTypeResponse;

import java.util.List;
import java.util.UUID;

public interface ServiceTypeService {

    List<ServiceTypeResponse> findAll();

    ServiceTypeResponse getById(UUID id);

    ServiceTypeResponse create(ServiceTypeRequest request);

    ServiceTypeResponse update(UUID id, ServiceTypeRequest request);

    void delete(UUID id);
}
