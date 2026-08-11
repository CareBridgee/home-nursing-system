package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.profile.ProfileMedicationRequest;
import iti.jets.java.homenursing.dto.profile.ProfileMedicationResponse;
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
import iti.jets.java.homenursing.util.CatalogEntryCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileMedicationServiceImpl implements ProfileMedicationService {

    private static final int MAX_NAME_LENGTH = 255;

    private final ProfileMedicationRepository profileMedicationRepository;
    private final ProfileMedicationMapper profileMedicationMapper;
    private final ProfileService profileService;
    private final MedicationRepository medicationRepository;
    private final CatalogEntryCreator catalogEntryCreator;

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

        Medication medication = resolveMedication(request);

        if (profileMedicationRepository.existsByProfileIdAndMedicationId(profileId, medication.getId())) {
            throw new BadRequestException("Medication already linked to this profile");
        }

        ProfileMedication profileMedication = ProfileMedication.builder()
                .profile(profile)
                .medication(medication)
                .build();

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

    private Medication resolveMedication(ProfileMedicationRequest request) {
        if (request.medicationId() != null) {
            return medicationRepository.findById(request.medicationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medication not found"));
        }

        String name = normalizeName(request.name(), "Provide either medication id or name to add a medication");
        return medicationRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> createUserMedication(name));
    }

    private Medication createUserMedication(String name) {
        try {
            return catalogEntryCreator.createMedication(name);
        } catch (DataIntegrityViolationException e) {
            return medicationRepository.findByNameIgnoreCase(name)
                    .orElseThrow(() -> new IllegalStateException("Failed to create medication: " + name, e));
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
    public List<ProfileMedicationResponse> listByProfile(UUID profileId) {
        profileService.getProfile(profileId); // existence check only, no ownership
        return profileMedicationRepository.findByProfileId(profileId).stream()
                .map(profileMedicationMapper::toResponse)
                .toList();
    }
}