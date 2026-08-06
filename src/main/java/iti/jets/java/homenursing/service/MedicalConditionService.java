package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.MedicalConditionRequest;
import iti.jets.java.homenursing.dto.MedicalConditionResponse;
import iti.jets.java.homenursing.entity.enums.CatalogSource;

import java.util.List;
import java.util.UUID;

public interface MedicalConditionService {

    List<MedicalConditionResponse> findAll();

    List<MedicalConditionResponse> findAll(CatalogSource source);

    MedicalConditionResponse getById(UUID id);

    MedicalConditionResponse create(MedicalConditionRequest request);

    MedicalConditionResponse update(UUID id, MedicalConditionRequest request);

    void delete(UUID id);
}
