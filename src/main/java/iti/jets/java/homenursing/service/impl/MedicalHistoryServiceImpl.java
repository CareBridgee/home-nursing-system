package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.MedicalHistoryRequest;
import iti.jets.java.homenursing.dto.MedicalHistoryResponse;
import iti.jets.java.homenursing.entity.MedicalHistory;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.MedicalHistoryMapper;
import iti.jets.java.homenursing.repository.MedicalHistoryRepository;
import iti.jets.java.homenursing.service.MedicalHistoryService;
import iti.jets.java.homenursing.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicalHistoryServiceImpl implements MedicalHistoryService {

    private final MedicalHistoryRepository medicalHistoryRepository;
    private final MedicalHistoryMapper medicalHistoryMapper;
    private final ProfileService profileService;

    @Override
    @Transactional(readOnly = true)
    public List<MedicalHistoryResponse> listByProfile(UUID profileId, UUID userId) {
        profileService.getOwnedProfileEntity(profileId, userId);
        return medicalHistoryRepository.findByProfileIdOrderByCreatedAtDesc(profileId)
                .stream()
                .map(medicalHistoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalHistoryResponse getById(UUID id, UUID userId) {
        MedicalHistory medicalHistory = loadOwned(id, userId);
        return medicalHistoryMapper.toResponse(medicalHistory);
    }

    @Override
    @Transactional
    public MedicalHistoryResponse create(UUID profileId, UUID userId, MedicalHistoryRequest request) {
        Profile profile = profileService.getOwnedProfileEntity(profileId, userId);
        MedicalHistory medicalHistory = medicalHistoryMapper.toEntity(request);
        medicalHistory.setProfile(profile);
        MedicalHistory saved = medicalHistoryRepository.save(medicalHistory);
        return medicalHistoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MedicalHistoryResponse update(UUID id, UUID userId, MedicalHistoryRequest request) {
        MedicalHistory medicalHistory = loadOwned(id, userId);
        if (request.getType() != null) medicalHistory.setType(request.getType());
        if (request.getDescription() != null) medicalHistory.setDescription(request.getDescription());
        MedicalHistory saved = medicalHistoryRepository.save(medicalHistory);
        return medicalHistoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        MedicalHistory medicalHistory = loadOwned(id, userId);
        medicalHistoryRepository.delete(medicalHistory);
    }

    private MedicalHistory loadOwned(UUID id, UUID userId) {
        MedicalHistory medicalHistory = medicalHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical history not found: " + id));
        if (!medicalHistory.getProfile().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Medical history not found: " + id);
        }
        return medicalHistory;
    }
}
