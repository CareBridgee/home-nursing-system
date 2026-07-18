package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.MedicalConditionResponse;
import iti.jets.java.homenursing.entity.MedicalCondition;
import iti.jets.java.homenursing.mapper.MedicalConditionMapper;
import iti.jets.java.homenursing.repository.MedicalConditionRepository;
import iti.jets.java.homenursing.service.MedicalConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
