package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.MedicationResponse;
import iti.jets.java.homenursing.entity.Medication;
import iti.jets.java.homenursing.mapper.MedicationMapper;
import iti.jets.java.homenursing.repository.MedicationRepository;
import iti.jets.java.homenursing.service.MedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
