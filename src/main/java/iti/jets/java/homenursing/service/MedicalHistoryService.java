package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.MedicalHistoryRequest;
import iti.jets.java.homenursing.dto.MedicalHistoryResponse;

import java.util.List;
import java.util.UUID;

public interface MedicalHistoryService {

    List<MedicalHistoryResponse> listByProfile(UUID profileId, UUID userId);

    MedicalHistoryResponse getById(UUID id, UUID userId);

    MedicalHistoryResponse create(UUID profileId, UUID userId, MedicalHistoryRequest request);

    MedicalHistoryResponse update(UUID id, UUID userId, MedicalHistoryRequest request);

    void delete(UUID id, UUID userId);
}
