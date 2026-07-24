package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestRequest;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyNurseServiceRequestResponse;

import java.util.List;
import java.util.UUID;

public interface ServiceRequestService {

    NearbyServiceRequestResponse createRequest( NearbyServiceRequestRequest request);

    List<NearbyNurseServiceRequestResponse> listNearbyForNurse(UUID userId);

    void cancelRequest(UUID serviceRequestId, UUID userId);
}
