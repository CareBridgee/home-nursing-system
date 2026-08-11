package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.profile.ProfileMedicationRequest;
import iti.jets.java.homenursing.dto.profile.ProfileMedicationResponse;

import java.util.List;
import java.util.UUID;

public interface ProfileMedicationService {

    List<ProfileMedicationResponse> listByProfile(UUID profileId, UUID userId);

    ProfileMedicationResponse addToProfile(UUID profileId, UUID userId, ProfileMedicationRequest request);

    void removeFromProfile(UUID profileId, UUID userId, UUID medicationId);

    List<ProfileMedicationResponse> listByProfile(UUID profileId);
}
