package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.EmergencyContactRequest;
import iti.jets.java.homenursing.dto.EmergencyContactResponse;

import java.util.List;
import java.util.UUID;

public interface EmergencyContactService {

    List<EmergencyContactResponse> listByProfile(UUID profileId, UUID userId);

    EmergencyContactResponse getById(UUID id, UUID userId);

    EmergencyContactResponse create(UUID profileId, UUID userId, EmergencyContactRequest request);

    EmergencyContactResponse update(UUID id, UUID userId, EmergencyContactRequest request);

    void delete(UUID id, UUID userId);
}
