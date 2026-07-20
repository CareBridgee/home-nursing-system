package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.nurse.NurseRegistrationRequest;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.nurse.NurseServiceRequest;
import iti.jets.java.homenursing.dto.nurse.NurseServiceResponse;
import iti.jets.java.homenursing.dto.nurse.NurseUpdateRequest;
import iti.jets.java.homenursing.dto.nurse.UpdateServicePriceRequest;

import java.util.List;
import java.util.UUID;

public interface NurseService {

    NurseResponse register(NurseRegistrationRequest request);

    NurseResponse updateProfile(UUID nurseId, NurseUpdateRequest request);

    NurseResponse getProfile(UUID nurseId);

    List<NurseResponse> listNurses();

    NurseServiceResponse addService(UUID nurseId, NurseServiceRequest request);

    NurseServiceResponse updateServicePrice(UUID nurseId, UUID serviceTypeId, UpdateServicePriceRequest request);

    void removeService(UUID nurseId, UUID serviceTypeId);
}
