package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.profile.ProfileAllergyRequest;
import iti.jets.java.homenursing.dto.profile.ProfileAllergyResponse;
import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ProfileAllergy;
import iti.jets.java.homenursing.entity.enums.AllergyType;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ProfileAllergyMapper;
import iti.jets.java.homenursing.repository.AllergyRepository;
import iti.jets.java.homenursing.repository.ProfileAllergyRepository;
import iti.jets.java.homenursing.service.ProfileAllergyService;
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
public class ProfileAllergyServiceImpl implements ProfileAllergyService {

    private static final int MAX_NAME_LENGTH = 255;

    private final ProfileAllergyRepository profileAllergyRepository;
    private final ProfileAllergyMapper profileAllergyMapper;
    private final ProfileService profileService;
    private final AllergyRepository allergyRepository;
    private final CatalogEntryCreator catalogEntryCreator;

    @Override
    @Transactional(readOnly = true)
    public List<ProfileAllergyResponse> listByProfile(UUID profileId, UUID userId) {
        profileService.getOwnedProfileEntity(profileId, userId);
        return profileAllergyRepository.findByProfileId(profileId).stream()
                .map(profileAllergyMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProfileAllergyResponse addToProfile(UUID profileId, UUID userId, ProfileAllergyRequest request) {
        Profile profile = profileService.getOwnedProfileEntity(profileId, userId);

        Allergy allergy = resolveAllergy(request);

        if (profileAllergyRepository.existsByProfileIdAndAllergyId(profileId, allergy.getId())) {
            throw new BadRequestException("Allergy already linked to this profile");
        }

        ProfileAllergy profileAllergy = ProfileAllergy.builder()
                .profile(profile)
                .allergy(allergy)
                .build();

        ProfileAllergy saved = profileAllergyRepository.save(profileAllergy);
        return profileAllergyMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void removeFromProfile(UUID profileId, UUID userId, UUID allergyId) {
        profileService.getOwnedProfileEntity(profileId, userId);

        ProfileAllergy profileAllergy = profileAllergyRepository.findByProfileIdAndAllergyId(profileId, allergyId)
                .orElseThrow(() -> new ResourceNotFoundException("Allergy link not found"));

        profileAllergyRepository.delete(profileAllergy);
    }

    private Allergy resolveAllergy(ProfileAllergyRequest request) {
        if (request.allergyId() != null) {
            return allergyRepository.findById(request.allergyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Allergy not found"));
        }

        String name = normalizeName(request.name(), "Provide either allergy id or name to add an allergy");
        return allergyRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> createUserAllergy(name, request.type()));
    }

    private Allergy createUserAllergy(String name, AllergyType type) {
        try {
            return catalogEntryCreator.createAllergy(name, type);
        } catch (DataIntegrityViolationException e) {
            return allergyRepository.findByNameIgnoreCase(name)
                    .orElseThrow(() -> new IllegalStateException("Failed to create allergy: " + name, e));
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
    public List<ProfileAllergyResponse> listByProfile(UUID profileId) {
        profileService.getProfile(profileId); // existence check only, no ownership
        return profileAllergyRepository.findByProfileId(profileId).stream()
                .map(profileAllergyMapper::toResponse)
                .toList();
    }
}