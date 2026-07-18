package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.ServiceTypeRequest;
import iti.jets.java.homenursing.dto.ServiceTypeResponse;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ServiceTypeMapper;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.service.ServiceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceTypeServiceImpl implements ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceTypeMapper serviceTypeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ServiceTypeResponse> findAll() {
        return serviceTypeRepository.findAll().stream()
                .map(serviceTypeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ServiceTypeResponse create(ServiceTypeRequest request) {
        ServiceType saved = serviceTypeRepository.save(serviceTypeMapper.toEntity(request));
        return serviceTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ServiceTypeResponse update(UUID id, ServiceTypeRequest request) {
        ServiceType entity = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + id));
        ServiceType updated = serviceTypeMapper.toEntity(request);
        updated.setId(entity.getId());
        updated.setCreatedAt(entity.getCreatedAt());
        ServiceType saved = serviceTypeRepository.save(updated);
        return serviceTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!serviceTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Service type not found: " + id);
        }
        serviceTypeRepository.deleteById(id);
    }
}
