package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.ProfileMedicationRequest;
import iti.jets.java.homenursing.dto.ProfileMedicationResponse;

import java.util.List;
import java.util.UUID;

public interface ProfileMedicationService {

    List<ProfileMedicationResponse> listByProfile(UUID profileId, UUID userId);

    ProfileMedicationResponse addToProfile(UUID profileId, UUID userId, ProfileMedicationRequest request);

    void removeFromProfile(UUID profileId, UUID userId, UUID medicationId);
}
