package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.ProfileRequest;
import iti.jets.java.homenursing.dto.ProfileResponse;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ProfileMapper;
import iti.jets.java.homenursing.repository.ProfileRepository;
import iti.jets.java.homenursing.service.CloudinaryService;
import iti.jets.java.homenursing.service.ProfileService;
import iti.jets.java.homenursing.util.ProfileImageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public Profile createDefaultProfile(User user) {
        Profile profile = Profile.builder()
                .user(user)
                .isPrimary(true)
                .isDeleted(false)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
        profile = profileRepository.save(profile);
        if (user.getProfiles() != null) {
            user.getProfiles().add(profile);
        }
        return profile;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getDefaultProfile(UUID userId) {
        Profile profile = profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Default profile not found for user " + userId));
        return toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileResponse> listProfiles(UUID userId) {
        return profileRepository.findByUserIdAndIsDeletedFalse(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getOwnedProfile(UUID profileId, UUID userId) {
        Profile profile = loadOwnedProfile(profileId, userId);
        return toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Profile getOwnedProfileEntity(UUID profileId, UUID userId) {
        return loadOwnedProfile(profileId, userId);
    }

    @Override
    @Transactional
    public ProfileResponse createFamilyProfile(UUID userId, ProfileRequest request) {
        Profile primary = profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Default profile not found for user " + userId));

        Profile profile = profileMapper.toEntity(request);
        profile.setUser(primary.getUser());
        profile.setIsPrimary(false);
        profile.setIsDeleted(false);
        applyProfileImage(profile, request);

        Profile saved = profileRepository.save(profile);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID profileId, UUID userId, ProfileRequest request) {
        Profile profile = loadOwnedProfile(profileId, userId);

        if (request.getRelationship() != null) profile.setRelationship(request.getRelationship());
        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null) profile.setLastName(request.getLastName());
        if (request.getDateOfBirth() != null) profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getBloodType() != null) profile.setBloodType(request.getBloodType());
        if (request.getHeight() != null) profile.setHeight(request.getHeight());
        if (request.getWeight() != null) profile.setWeight(request.getWeight());
        if (request.getMobilityStatus() != null) profile.setMobilityStatus(request.getMobilityStatus());
        if (request.getMobilityNotes() != null) profile.setMobilityNotes(request.getMobilityNotes());
        if (request.getPreviousSurgeries() != null) profile.setPreviousSurgeries(request.getPreviousSurgeries());
        if (request.getPreviousHospitalizations() != null) {
            profile.setPreviousHospitalizations(request.getPreviousHospitalizations());
        }
        applyProfileImage(profile, request);

        Profile saved = profileRepository.save(profile);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteFamilyProfile(UUID profileId, UUID userId) {
        Profile profile = loadOwnedProfile(profileId, userId);
        if (Boolean.TRUE.equals(profile.getIsPrimary())) {
            throw new BadRequestException("Cannot delete the primary profile");
        }
        profile.setIsDeleted(true);
        profileRepository.save(profile);
    }

    private Profile loadOwnedProfile(UUID profileId, UUID userId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + profileId));
        if (!profile.getUser().getId().equals(userId) || Boolean.TRUE.equals(profile.getIsDeleted())) {
            throw new ResourceNotFoundException("Profile not found: " + profileId);
        }
        return profile;
    }

    @Override
    public Profile getProfile(UUID profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + profileId));
    }

    private void applyProfileImage(Profile profile, ProfileRequest request) {
        if (request.getProfileImageUrl() != null) {
            profile.setProfileImageUrl(request.getProfileImageUrl());
        }
        uploadProfileImage(profile, request.getProfileImage());
    }

    private void uploadProfileImage(Profile profile, MultipartFile profileImage) {
        if (profileImage != null && !profileImage.isEmpty()) {
            profile.setProfileImageUrl(cloudinaryService.upload(profileImage));
        }
    }

    private ProfileResponse toResponse(Profile profile) {
        ProfileResponse response = profileMapper.toResponse(profile);
        response.setProfileImageUrl(ProfileImageUtil.resolveProfileImageUrl(profile));
        return response;
    }
}
