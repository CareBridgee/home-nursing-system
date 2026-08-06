package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.nurse.NearbyNurse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestRequest;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyNurseServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.ServiceRequestDetailsResponse;
import iti.jets.java.homenursing.dto.servicerequest.ServiceRequestHistoryResponse;
import iti.jets.java.homenursing.dto.servicerequest.VisitCodeResponse;

import java.util.List;
import java.util.UUID;

public interface ServiceRequestService {

    NearbyServiceRequestResponse createRequest( NearbyServiceRequestRequest request);

    List<NearbyNurseServiceRequestResponse> listNearbyForNurse(UUID userId);

    List<NearbyNurse> getNearbyNursesForRequest(UUID serviceRequestId, UUID userId);

    void cancelRequest(UUID serviceRequestId, UUID userId);

    VisitCodeResponse generateVisitCode(UUID serviceRequestId, UUID userId);

    void completeRequest(UUID serviceRequestId, String visitCode, UUID userId);

    ServiceRequestDetailsResponse getDetails(UUID serviceRequestId, UUID userId);

    List<ServiceRequestHistoryResponse> listConfirmedHistory(UUID userId);
}
