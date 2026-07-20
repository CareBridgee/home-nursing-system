package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.MedicationRequest;
import iti.jets.java.homenursing.dto.MedicationResponse;

import java.util.List;
import java.util.UUID;

public interface MedicationService {

    List<MedicationResponse> findAll();

    MedicationResponse getById(UUID id);

    MedicationResponse create(MedicationRequest request);

    MedicationResponse update(UUID id, MedicationRequest request);

    void delete(UUID id);
}
