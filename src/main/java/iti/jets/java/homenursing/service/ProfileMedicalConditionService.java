package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.profile.ProfileMedicalConditionRequest;
import iti.jets.java.homenursing.dto.profile.ProfileMedicalConditionResponse;

import java.util.List;
import java.util.UUID;

public interface ProfileMedicalConditionService {

    List<ProfileMedicalConditionResponse> listByProfile(UUID profileId, UUID userId);

    ProfileMedicalConditionResponse addToProfile(UUID profileId, UUID userId, ProfileMedicalConditionRequest request);

    void removeFromProfile(UUID profileId, UUID userId, UUID medicalConditionId);

    List<ProfileMedicalConditionResponse> listByProfile(UUID profileId);
}
