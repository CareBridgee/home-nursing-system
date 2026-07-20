package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.ProfileMedicationRequest;
import iti.jets.java.homenursing.dto.ProfileMedicationResponse;
import iti.jets.java.homenursing.entity.Medication;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ProfileMedication;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ProfileMedicationMapper;
import iti.jets.java.homenursing.repository.MedicationRepository;
import iti.jets.java.homenursing.repository.ProfileMedicationRepository;
import iti.jets.java.homenursing.service.ProfileMedicationService;
import iti.jets.java.homenursing.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileMedicationServiceImpl implements ProfileMedicationService {

    private final ProfileMedicationRepository profileMedicationRepository;
    private final ProfileMedicationMapper profileMedicationMapper;
    private final ProfileService profileService;
    private final MedicationRepository medicationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProfileMedicationResponse> listByProfile(UUID profileId, UUID userId) {
        profileService.getOwnedProfileEntity(profileId, userId);
        return profileMedicationRepository.findByProfileId(profileId).stream()
                .map(profileMedicationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProfileMedicationResponse addToProfile(UUID profileId, UUID userId, ProfileMedicationRequest request) {
        Profile profile = profileService.getOwnedProfileEntity(profileId, userId);

        Medication medication = medicationRepository.findById(request.medicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Medication not found"));

        if (profileMedicationRepository.existsByProfileIdAndMedicationId(profileId, request.medicationId())) {
            throw new BadRequestException("Medication already linked to this profile");
        }

        ProfileMedication profileMedication = profileMedicationMapper.toEntity(request);
        profileMedication.setProfile(profile);
        profileMedication.setMedication(medication);

        ProfileMedication saved = profileMedicationRepository.save(profileMedication);
        return profileMedicationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void removeFromProfile(UUID profileId, UUID userId, UUID medicationId) {
        profileService.getOwnedProfileEntity(profileId, userId);

        ProfileMedication profileMedication = profileMedicationRepository.findByProfileIdAndMedicationId(profileId, medicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Medication link not found"));

        profileMedicationRepository.delete(profileMedication);
    }
}
