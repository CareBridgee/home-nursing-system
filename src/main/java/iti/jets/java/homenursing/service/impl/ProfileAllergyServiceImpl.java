package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.ProfileAllergyRequest;
import iti.jets.java.homenursing.dto.ProfileAllergyResponse;
import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ProfileAllergy;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ProfileAllergyMapper;
import iti.jets.java.homenursing.repository.AllergyRepository;
import iti.jets.java.homenursing.repository.ProfileAllergyRepository;
import iti.jets.java.homenursing.service.ProfileAllergyService;
import iti.jets.java.homenursing.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileAllergyServiceImpl implements ProfileAllergyService {

    private final ProfileAllergyRepository profileAllergyRepository;
    private final ProfileAllergyMapper profileAllergyMapper;
    private final ProfileService profileService;
    private final AllergyRepository allergyRepository;

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

        Allergy allergy = allergyRepository.findById(request.allergyId())
                .orElseThrow(() -> new ResourceNotFoundException("Allergy not found"));

        if (profileAllergyRepository.existsByProfileIdAndAllergyId(profileId, request.allergyId())) {
            throw new BadRequestException("Allergy already linked to this profile");
        }

        ProfileAllergy profileAllergy = profileAllergyMapper.toEntity(request);
        profileAllergy.setProfile(profile);
        profileAllergy.setAllergy(allergy);

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
}
