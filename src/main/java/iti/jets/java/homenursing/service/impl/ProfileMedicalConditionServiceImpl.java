package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.ProfileMedicalConditionRequest;
import iti.jets.java.homenursing.dto.ProfileMedicalConditionResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileMedicalConditionServiceImpl implements ProfileMedicalConditionService {

    private final ProfileMedicalConditionRepository profileMedicalConditionRepository;
    private final ProfileMedicalConditionMapper profileMedicalConditionMapper;
    private final ProfileService profileService;
    private final MedicalConditionRepository medicalConditionRepository;

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

        MedicalCondition condition = medicalConditionRepository.findById(request.medicalConditionId())
                .orElseThrow(() -> new ResourceNotFoundException("Medical condition not found"));

        if (profileMedicalConditionRepository.existsByProfileIdAndMedicalConditionId(profileId, request.medicalConditionId())) {
            throw new BadRequestException("Medical condition already linked to this profile");
        }

        ProfileMedicalCondition profileMedicalCondition = profileMedicalConditionMapper.toEntity(request);
        profileMedicalCondition.setProfile(profile);
        profileMedicalCondition.setMedicalCondition(condition);

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
}
