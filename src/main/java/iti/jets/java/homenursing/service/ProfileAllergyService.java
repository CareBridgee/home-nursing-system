package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.ProfileAllergyRequest;
import iti.jets.java.homenursing.dto.ProfileAllergyResponse;

import java.util.List;
import java.util.UUID;

public interface ProfileAllergyService {

    List<ProfileAllergyResponse> listByProfile(UUID profileId, UUID userId);

    ProfileAllergyResponse addToProfile(UUID profileId, UUID userId, ProfileAllergyRequest request);

    void removeFromProfile(UUID profileId, UUID userId, UUID allergyId);
}
