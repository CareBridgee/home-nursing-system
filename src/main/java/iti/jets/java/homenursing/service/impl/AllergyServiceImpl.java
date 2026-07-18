package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.AllergyResponse;
import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.mapper.AllergyMapper;
import iti.jets.java.homenursing.repository.AllergyRepository;
import iti.jets.java.homenursing.service.AllergyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
