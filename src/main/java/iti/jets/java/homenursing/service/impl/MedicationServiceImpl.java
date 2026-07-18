package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.MedicationRequest;
import iti.jets.java.homenursing.dto.MedicationResponse;
import iti.jets.java.homenursing.entity.Medication;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.MedicationMapper;
import iti.jets.java.homenursing.repository.MedicationRepository;
import iti.jets.java.homenursing.service.MedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicationServiceImpl implements MedicationService {

    private final MedicationRepository medicationRepository;
    private final MedicationMapper medicationMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MedicationResponse> findAll() {
        return medicationRepository.findAll().stream()
                .map(medicationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MedicationResponse create(MedicationRequest request) {
        Medication saved = medicationRepository.save(medicationMapper.toEntity(request));
        return medicationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MedicationResponse update(UUID id, MedicationRequest request) {
        Medication entity = medicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication not found: " + id));
        Medication updated = medicationMapper.toEntity(request);
        updated.setId(entity.getId());
        updated.setCreatedAt(entity.getCreatedAt());
        Medication saved = medicationRepository.save(updated);
        return medicationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!medicationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Medication not found: " + id);
        }
        medicationRepository.deleteById(id);
    }
}
