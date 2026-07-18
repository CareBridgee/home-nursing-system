package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.AllergyRequest;
import iti.jets.java.homenursing.dto.AllergyResponse;

import java.util.List;
import java.util.UUID;

public interface AllergyService {

    List<AllergyResponse> findAll();

    AllergyResponse create(AllergyRequest request);

    AllergyResponse update(UUID id, AllergyRequest request);

    void delete(UUID id);
}
