package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.AllergyRequest;
import iti.jets.java.homenursing.dto.AllergyResponse;
import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.AllergyMapper;
import iti.jets.java.homenursing.repository.AllergyRepository;
import iti.jets.java.homenursing.service.AllergyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AllergyServiceImpl implements AllergyService {

    private final AllergyRepository allergyRepository;
    private final AllergyMapper allergyMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AllergyResponse> findAll() {
        return allergyRepository.findAll().stream()
                .map(allergyMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AllergyResponse create(AllergyRequest request) {
        Allergy entity = allergyMapper.toEntity(request);
        return allergyMapper.toResponse(allergyRepository.save(entity));
    }

    @Override
    @Transactional
    public AllergyResponse update(UUID id, AllergyRequest request) {
        Allergy entity = allergyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allergy not found: " + id));
        entity.setName(request.name());
        entity.setType(request.type());
        return allergyMapper.toResponse(allergyRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!allergyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Allergy not found: " + id);
        }
        allergyRepository.deleteById(id);
    }
}
