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
        ServiceType entity = serviceTypeMapper.toEntity(request);
        return serviceTypeMapper.toResponse(serviceTypeRepository.save(entity));
    }

    @Override
    @Transactional
    public ServiceTypeResponse update(UUID id, ServiceTypeRequest request) {
        ServiceType entity = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + id));
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        entity.setBasePrice(request.basePrice());
        return serviceTypeMapper.toResponse(serviceTypeRepository.save(entity));
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
