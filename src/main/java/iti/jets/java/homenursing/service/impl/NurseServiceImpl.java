package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.nurse.NurseRegistrationRequest;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.nurse.NurseServiceBatchResult;
import iti.jets.java.homenursing.dto.nurse.NurseServiceRequest;
import iti.jets.java.homenursing.dto.nurse.NurseServiceResponse;
import iti.jets.java.homenursing.dto.nurse.NurseUpdateRequest;
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
import iti.jets.java.homenursing.service.CloudinaryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
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
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public NurseResponse register(UUID userId, NurseRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Nurse nurse = nurseRepository.findByUser_Id(user.getId()).orElse(null);

        if (nurse == null) {
            if (request.getNationalId() != null && nurseRepository.existsByNationalId(request.getNationalId())) {
                throw new BadRequestException("National ID already exists");
            }
            nurse = nurseMapper.toEntity(request, user);
        } else {
            if (request.getNationalId() != null &&
                    !request.getNationalId().equals(nurse.getNationalId()) &&
                    nurseRepository.existsByNationalId(request.getNationalId())) {
                throw new BadRequestException("National ID already exists");
            }
            nurse.setNationalId(request.getNationalId());
            nurse.setLicenseNumber(request.getLicenseNumber());
            nurse.setSpecialization(request.getSpecialization());
            nurse.setYearsOfExperience(request.getYearsOfExperience());
            nurse.setBio(request.getBio());
        }

        uploadDocuments(nurse, request.getNationalIdFront(), request.getNationalIdBack(),
                request.getLicenseImage(), request.getProfessionalCertificate());
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
        uploadDocuments(nurse, request.getNationalIdFront(), request.getNationalIdBack(),
                request.getLicenseImage(), request.getProfessionalCertificate());
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
    public NurseServiceBatchResult addServices(UUID nurseId, UUID userId, List<NurseServiceRequest> requests) {
        Nurse nurse = getOwnedNurseOrThrow(nurseId, userId);

        LinkedHashSet<UUID> serviceTypeIds = new LinkedHashSet<>();
        for (NurseServiceRequest request : requests) {
            if (request.getServiceTypeId() != null) {
                serviceTypeIds.add(request.getServiceTypeId());
            }
        }

        List<NurseServiceResponse> added = new ArrayList<>();
        List<NurseServiceBatchResult.BatchFailure> failed = new ArrayList<>();

        for (UUID serviceTypeId : serviceTypeIds) {
            ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId).orElse(null);
            if (serviceType == null) {
                failed.add(NurseServiceBatchResult.BatchFailure.builder()
                        .serviceTypeId(serviceTypeId)
                        .reason("Service type not found")
                        .build());
                continue;
            }

            NurseService link = nurseServiceRepository
                    .findByNurse_IdAndServiceType_Id(nurseId, serviceTypeId)
                    .orElseGet(() -> NurseService.builder()
                            .nurse(nurse)
                            .serviceType(serviceType)
                            .build());

            link.setIsActive(true);
            added.add(nurseMapper.toServiceResponse(nurseServiceRepository.save(link)));
        }

        for (NurseServiceRequest request : requests) {
            if (request.getServiceTypeId() == null) {
                failed.add(NurseServiceBatchResult.BatchFailure.builder()
                        .serviceTypeId(null)
                        .reason("Service type id is required")
                        .build());
            }
        }

        return NurseServiceBatchResult.builder()
                .added(added)
                .failed(failed)
                .build();
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

    private void uploadDocuments(Nurse nurse, MultipartFile nationalIdFront, MultipartFile nationalIdBack,
                                 MultipartFile licenseImage, MultipartFile professionalCertificate) {
        if (hasContent(nationalIdFront)) {
            nurse.setNationalIdFrontUrl(cloudinaryService.upload(nationalIdFront));
        }
        if (hasContent(nationalIdBack)) {
            nurse.setNationalIdBackUrl(cloudinaryService.upload(nationalIdBack));
        }
        if (hasContent(licenseImage)) {
            nurse.setLicenseImageUrl(cloudinaryService.upload(licenseImage));
        }
        if (hasContent(professionalCertificate)) {
            nurse.setProfessionalCertificateUrl(cloudinaryService.upload(professionalCertificate));
        }
    }

    private boolean hasContent(MultipartFile file) {
        return file != null && !file.isEmpty();
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
