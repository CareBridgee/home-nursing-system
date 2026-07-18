package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.MedicalConditionRequest;
import iti.jets.java.homenursing.dto.MedicalConditionResponse;

import java.util.List;
import java.util.UUID;

public interface MedicalConditionService {

    List<MedicalConditionResponse> findAll();

    MedicalConditionResponse create(MedicalConditionRequest request);

    MedicalConditionResponse update(UUID id, MedicalConditionRequest request);

    void delete(UUID id);
}
