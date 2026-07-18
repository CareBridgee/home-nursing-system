package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.ProfileRequest;
import iti.jets.java.homenursing.dto.ProfileResponse;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ProfileMapper;
import iti.jets.java.homenursing.repository.ProfileRepository;
import iti.jets.java.homenursing.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    @Override
    @Transactional
    public Profile createDefaultProfile(User user) {
        Profile profile = Profile.builder()
                .user(user)
                .relationship(null)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .build();
        return profileRepository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getDefaultProfile(UUID userId) {
        Profile profile = profileRepository.findByUserIdAndRelationshipIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Default profile not found for user " + userId));
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileResponse> listProfiles(UUID userId) {
        return profileRepository.findByUserId(userId)
                .stream()
                .map(profileMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getOwnedProfile(UUID profileId, UUID userId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + profileId));
        if (!profile.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Profile not found: " + profileId);
        }
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public ProfileResponse createFamilyProfile(UUID userId, ProfileRequest request) {
        Profile defaultProfile = profileRepository.findByUserIdAndRelationshipIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Default profile not found for user " + userId));

        Profile profile = profileMapper.toEntity(request);
        profile.setUser(defaultProfile.getUser());
        profile.setRelationship(defaultProfile.getId());

        Profile saved = profileRepository.save(profile);
        return profileMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID profileId, UUID userId, ProfileRequest request) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + profileId));
        if (!profile.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Profile not found: " + profileId);
        }

        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null) profile.setLastName(request.getLastName());
        if (request.getDateOfBirth() != null) profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getBloodType() != null) profile.setBloodType(request.getBloodType());
        if (request.getHeightCm() != null) profile.setHeightCm(request.getHeightCm());
        if (request.getWeightKg() != null) profile.setWeightKg(request.getWeightKg());

        Profile saved = profileRepository.save(profile);
        return profileMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteFamilyProfile(UUID profileId, UUID userId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + profileId));
        if (!profile.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Profile not found: " + profileId);
        }
        if (profile.getRelationship() == null) {
            throw new IllegalArgumentException("Cannot delete the default profile");
        }
        profileRepository.delete(profile);
    }
}
