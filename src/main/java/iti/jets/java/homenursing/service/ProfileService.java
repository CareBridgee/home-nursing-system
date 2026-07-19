package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.ProfileRequest;
import iti.jets.java.homenursing.dto.ProfileResponse;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.User;

import java.util.List;
import java.util.UUID;

public interface ProfileService {

    Profile createDefaultProfile(User user);

    ProfileResponse getDefaultProfile(UUID userId);

    List<ProfileResponse> listProfiles(UUID userId);

    ProfileResponse getOwnedProfile(UUID profileId, UUID userId);

    ProfileResponse createFamilyProfile(UUID userId, ProfileRequest request);

    ProfileResponse updateProfile(UUID profileId, UUID userId, ProfileRequest request);

    void deleteFamilyProfile(UUID profileId, UUID userId);
}
