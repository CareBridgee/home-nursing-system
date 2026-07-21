package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.nurse.NurseRegistrationRequest;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.nurse.NurseServiceRequest;
import iti.jets.java.homenursing.dto.nurse.NurseServiceResponse;
import iti.jets.java.homenursing.dto.nurse.NurseUpdateRequest;
import iti.jets.java.homenursing.dto.nurse.UpdateServicePriceRequest;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseService;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.NurseMapper;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.NurseServiceRepository;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class NurseServiceImpl implements iti.jets.java.homenursing.service.NurseService {

    private final NurseRepository nurseRepository;
    private final UserRepository userRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final NurseServiceRepository nurseServiceRepository;
    private final NurseMapper nurseMapper;

    @Override
    @Transactional
    public NurseResponse register(UUID userId, NurseRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (nurseRepository.existsByUser_Id(user.getId())) {
            throw new BadRequestException("This user already has a nurse profile");
        }

        if (request.getNationalId() != null && nurseRepository.existsByNationalId(request.getNationalId())) {
            throw new BadRequestException("National ID already exists");
        }

        Nurse nurse = nurseMapper.toEntity(request, user);
        return toProfileResponse(nurseRepository.save(nurse));
    }

    @Override
    @Transactional
    public NurseResponse updateProfile(UUID nurseId, UUID userId, NurseUpdateRequest request) {
        Nurse nurse = getOwnedNurseOrThrow(nurseId, userId);

        if (nurse.getVerificationStatus() == VerificationStatus.REJECTED) {
            nurse.setVerificationStatus(VerificationStatus.UNDER_REVIEW);
            nurse.setRejectionReason(null);
            nurse.setRejectionDetail(null);
        }

        nurseMapper.updateEntity(request, nurse);
        return toProfileResponse(nurseRepository.save(nurse));
    }

    @Override
    public NurseResponse getProfile(UUID nurseId) {
        return toProfileResponse(getNurseOrThrow(nurseId));
    }

    @Override
    public List<NurseResponse> listNurses() {
        return nurseRepository.findAll().stream()
                .map(nurseMapper::toSimpleResponse)
                .toList();
    }

    @Override
    @Transactional
    public NurseServiceResponse addService(UUID nurseId, UUID userId, NurseServiceRequest request) {
        Nurse nurse = getOwnedNurseOrThrow(nurseId, userId);
        ServiceType serviceType = getServiceTypeOrThrow(request.getServiceTypeId());

        NurseService link = nurseServiceRepository
                .findByNurse_IdAndServiceType_Id(nurseId, request.getServiceTypeId())
                .orElseGet(() -> NurseService.builder()
                        .nurse(nurse)
                        .serviceType(serviceType)
                        .build());

        link.setCustomPrice(request.getCustomPrice());
        link.setIsActive(true);

        return nurseMapper.toServiceResponse(nurseServiceRepository.save(link));
    }

    @Override
    @Transactional
    public NurseServiceResponse updateServicePrice(UUID nurseId, UUID userId, UUID serviceTypeId, UpdateServicePriceRequest request) {
        getOwnedNurseOrThrow(nurseId, userId);
        NurseService nurseService = nurseServiceRepository
                .findByNurse_IdAndServiceType_Id(nurseId, serviceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse service not found"));

        nurseService.setCustomPrice(request.getCustomPrice());
        return nurseMapper.toServiceResponse(nurseServiceRepository.save(nurseService));
    }

    @Override
    @Transactional
    public void removeService(UUID nurseId, UUID userId, UUID serviceTypeId) {
        getOwnedNurseOrThrow(nurseId, userId);
        NurseService nurseService = nurseServiceRepository
                .findByNurse_IdAndServiceType_Id(nurseId, serviceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse service not found"));
        nurseService.setIsActive(false);
        nurseServiceRepository.save(nurseService);
    }

    private Nurse getNurseOrThrow(UUID nurseId) {
        return nurseRepository.findById(nurseId)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse not found"));
    }

    private Nurse getOwnedNurseOrThrow(UUID nurseId, UUID userId) {
        Nurse nurse = getNurseOrThrow(nurseId);
        if (!nurse.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Nurse not found");
        }
        return nurse;
    }

    private ServiceType getServiceTypeOrThrow(UUID serviceTypeId) {
        return serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Service type not found"));
    }

    private NurseResponse toProfileResponse(Nurse nurse) {
        List<NurseServiceResponse> services = nurseServiceRepository.findAllByNurse_Id(nurse.getId()).stream()
                .map(nurseMapper::toServiceResponse)
                .sorted(Comparator.comparing(NurseServiceResponse::getServiceName))
                .toList();

        return nurseMapper.toResponse(nurse, services);
    }

    @Override
    public List<NurseResponse> findVerifiedNursesByServiceTypeName(String serviceTypeName) {
        return nurseServiceRepository.findByServiceType_NameContainingIgnoreCaseAndIsActiveTrue(serviceTypeName)
                .stream()
                .map(NurseService::getNurse)
                .filter(nurse -> nurse.getVerificationStatus() == VerificationStatus.APPROVED)
                .distinct()
                .map(nurseMapper::toSimpleResponse)
                .toList();
    }
}
