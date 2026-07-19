package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.MedicalConditionRequest;
import iti.jets.java.homenursing.dto.MedicalConditionResponse;
import iti.jets.java.homenursing.entity.MedicalCondition;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.MedicalConditionMapper;
import iti.jets.java.homenursing.repository.MedicalConditionRepository;
import iti.jets.java.homenursing.service.MedicalConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicalConditionServiceImpl implements MedicalConditionService {

    private final MedicalConditionRepository medicalConditionRepository;
    private final MedicalConditionMapper medicalConditionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MedicalConditionResponse> findAll() {
        return medicalConditionRepository.findAll().stream()
                .map(medicalConditionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MedicalConditionResponse create(MedicalConditionRequest request) {
        MedicalCondition entity = medicalConditionMapper.toEntity(request);
        return medicalConditionMapper.toResponse(medicalConditionRepository.save(entity));
    }

    @Override
    @Transactional
    public MedicalConditionResponse update(UUID id, MedicalConditionRequest request) {
        MedicalCondition entity = medicalConditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical condition not found: " + id));
        entity.setName(request.name());
        entity.setDescription(request.description());
        return medicalConditionMapper.toResponse(medicalConditionRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!medicalConditionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Medical condition not found: " + id);
        }
        medicalConditionRepository.deleteById(id);
    }
}
