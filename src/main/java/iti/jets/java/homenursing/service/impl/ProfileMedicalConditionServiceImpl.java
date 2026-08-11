package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.profile.ProfileMedicalConditionRequest;
import iti.jets.java.homenursing.dto.profile.ProfileMedicalConditionResponse;
import iti.jets.java.homenursing.entity.MedicalCondition;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ProfileMedicalCondition;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ProfileMedicalConditionMapper;
import iti.jets.java.homenursing.repository.MedicalConditionRepository;
import iti.jets.java.homenursing.repository.ProfileMedicalConditionRepository;
import iti.jets.java.homenursing.service.ProfileMedicalConditionService;
import iti.jets.java.homenursing.service.ProfileService;
import iti.jets.java.homenursing.util.CatalogEntryCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileMedicalConditionServiceImpl implements ProfileMedicalConditionService {

    private static final int MAX_NAME_LENGTH = 255;

    private final ProfileMedicalConditionRepository profileMedicalConditionRepository;
    private final ProfileMedicalConditionMapper profileMedicalConditionMapper;
    private final ProfileService profileService;
    private final MedicalConditionRepository medicalConditionRepository;
    private final CatalogEntryCreator catalogEntryCreator;

    @Override
    @Transactional(readOnly = true)
    public List<ProfileMedicalConditionResponse> listByProfile(UUID profileId, UUID userId) {
        profileService.getOwnedProfileEntity(profileId, userId);
        return profileMedicalConditionRepository.findByProfileId(profileId).stream()
                .map(profileMedicalConditionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProfileMedicalConditionResponse addToProfile(UUID profileId, UUID userId, ProfileMedicalConditionRequest request) {
        Profile profile = profileService.getOwnedProfileEntity(profileId, userId);

        MedicalCondition condition = resolveCondition(request);

        if (profileMedicalConditionRepository.existsByProfileIdAndMedicalConditionId(profileId, condition.getId())) {
            throw new BadRequestException("Medical condition already linked to this profile");
        }

        ProfileMedicalCondition profileMedicalCondition = ProfileMedicalCondition.builder()
                .profile(profile)
                .medicalCondition(condition)
                .build();

        ProfileMedicalCondition saved = profileMedicalConditionRepository.save(profileMedicalCondition);
        return profileMedicalConditionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void removeFromProfile(UUID profileId, UUID userId, UUID medicalConditionId) {
        profileService.getOwnedProfileEntity(profileId, userId);

        ProfileMedicalCondition profileMedicalCondition = profileMedicalConditionRepository
                .findByProfileIdAndMedicalConditionId(profileId, medicalConditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical condition link not found"));

        profileMedicalConditionRepository.delete(profileMedicalCondition);
    }

    private MedicalCondition resolveCondition(ProfileMedicalConditionRequest request) {
        if (request.medicalConditionId() != null) {
            return medicalConditionRepository.findById(request.medicalConditionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medical condition not found"));
        }

        String name = normalizeName(request.name(), "Provide either medical condition id or name to add a condition");
        return medicalConditionRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> createUserCondition(name, request.description()));
    }

    private MedicalCondition createUserCondition(String name, String description) {
        try {
            return catalogEntryCreator.createMedicalCondition(name, description);
        } catch (DataIntegrityViolationException e) {
            return medicalConditionRepository.findByNameIgnoreCase(name)
                    .orElseThrow(() -> new IllegalStateException("Failed to create medical condition: " + name, e));
        }
    }

    private String normalizeName(String rawName, String blankMessage) {
        if (rawName == null || rawName.isBlank()) {
            throw new BadRequestException(blankMessage);
        }
        String name = rawName.trim();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BadRequestException("Name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        return name;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileMedicalConditionResponse> listByProfile(UUID profileId) {
        profileService.getProfile(profileId); // existence check only, no ownership
        return profileMedicalConditionRepository.findByProfileId(profileId).stream()
                .map(profileMedicalConditionMapper::toResponse)
                .toList();
    }
}