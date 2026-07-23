package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestRequest;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;

public interface ServiceRequestService {

    NearbyServiceRequestResponse createRequest( NearbyServiceRequestRequest request);
}
