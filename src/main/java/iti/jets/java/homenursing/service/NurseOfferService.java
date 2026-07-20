package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface NurseOfferService {

    NurseOfferResponse create(UUID userId, NurseOfferRequest request);

    List<NurseOfferResponse> listByServiceRequest(UUID serviceRequestId, UUID userId);

    NurseOfferResponse get(UUID id, UUID userId);

    NurseOfferResponse update(UUID id, UUID userId, NurseOfferUpdateRequest request);

    void delete(UUID id, UUID userId);
}
