package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.ServiceTypeRequest;
import iti.jets.java.homenursing.dto.ServiceTypeResponse;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ServiceTypeMapper;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.service.CloudinaryService;
import iti.jets.java.homenursing.service.ServiceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceTypeServiceImpl implements ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceTypeMapper serviceTypeMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public List<ServiceTypeResponse> findAll() {
        return serviceTypeRepository.findAll().stream()
                .map(serviceTypeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceTypeResponse getById(UUID id) {
        ServiceType entity = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + id));
        return serviceTypeMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public ServiceTypeResponse create(ServiceTypeRequest request) {
        ServiceType entity = serviceTypeMapper.toEntity(request);
        if (hasContent(request.image())) {
            validateImage(request.image());
            entity.setImageUrl(cloudinaryService.upload(request.image()));
        }
        return serviceTypeMapper.toResponse(serviceTypeRepository.save(entity));
    }

    @Override
    @Transactional
    public ServiceTypeResponse update(UUID id, ServiceTypeRequest request) {
        ServiceType entity = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + id));
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setCategory(request.category());
        entity.setMinimumDurationMinutes(request.minimumDurationMinutes());
        entity.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        entity.setBasePrice(request.basePrice());
        entity.setIncludedItems(request.includedItems());
        entity.setPreparationNote(request.preparationNote());

        if (hasContent(request.image())) {
            validateImage(request.image());
            String oldImageUrl = entity.getImageUrl();
            String newImageUrl = cloudinaryService.upload(request.image());
            cloudinaryService.delete(oldImageUrl);
            entity.setImageUrl(newImageUrl);
        }

        return serviceTypeMapper.toResponse(serviceTypeRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ServiceType entity = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found: " + id));
        serviceTypeRepository.delete(entity);
        serviceTypeRepository.flush();
        cloudinaryService.delete(entity.getImageUrl());
    }

    private boolean hasContent(MultipartFile image) {
        return image != null && !image.isEmpty();
    }

    private void validateImage(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Service type image must be an image file");
        }
    }
}
