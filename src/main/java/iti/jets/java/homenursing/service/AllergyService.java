package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.catalog.AllergyRequest;
import iti.jets.java.homenursing.dto.catalog.AllergyResponse;
import iti.jets.java.homenursing.entity.enums.CatalogSource;

import java.util.List;
import java.util.UUID;

public interface AllergyService {

    List<AllergyResponse> findAll();

    List<AllergyResponse> findAll(CatalogSource source);

    AllergyResponse getById(UUID id);

    AllergyResponse create(AllergyRequest request);

    AllergyResponse update(UUID id, AllergyRequest request);

    void delete(UUID id);
}
