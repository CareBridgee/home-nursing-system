package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.nurse.NurseRegistrationRequest;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.nurse.NurseServiceBatchResult;
import iti.jets.java.homenursing.dto.nurse.NurseServiceRequest;
import iti.jets.java.homenursing.dto.nurse.NurseUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface NurseService {

    NurseResponse register(UUID userId, NurseRegistrationRequest request);

    NurseResponse updateProfile(UUID nurseId, UUID userId, NurseUpdateRequest request);

    NurseResponse getProfile(UUID nurseId);

    List<NurseResponse> listNurses();

    NurseServiceBatchResult addServices(UUID nurseId, UUID userId, List<NurseServiceRequest> requests);

    void removeService(UUID nurseId, UUID userId, UUID serviceTypeId);

    List<NurseResponse> findVerifiedNursesByServiceTypeName(String serviceTypeName);
}
