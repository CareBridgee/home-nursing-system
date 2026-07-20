package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.nurse.NurseRejectionRequest;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;

import java.util.List;
import java.util.UUID;

public interface AdminNurseService {

    List<NurseResponse> listByStatus(VerificationStatus status);

    NurseResponse approve(UUID nurseId);

    NurseResponse reject(UUID nurseId, NurseRejectionRequest request);
}
