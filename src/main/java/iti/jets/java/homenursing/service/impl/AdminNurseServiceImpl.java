package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.nurse.NurseRejectionDetailsResponse;
import iti.jets.java.homenursing.dto.nurse.NurseRejectionRequest;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.nurse.NurseServiceResponse;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseRejectionDetail;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.NurseMapper;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.NurseServiceRepository;
import iti.jets.java.homenursing.service.AdminNurseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminNurseServiceImpl implements AdminNurseService {

    private final NurseRepository nurseRepository;
    private final NurseServiceRepository nurseServiceRepository;
    private final NurseMapper nurseMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NurseResponse> listByStatus(VerificationStatus status) {
        return nurseRepository.findByVerificationStatus(status).stream()
                .map(nurse -> {
                    List<NurseServiceResponse> services = fetchServices(nurse);
                    return nurseMapper.toResponse(nurse, services);
                })
                .toList();
    }

    @Override
    @Transactional
    public NurseResponse approve(UUID nurseId) {
        Nurse nurse = getNurseOrThrow(nurseId);
        nurse.setVerificationStatus(VerificationStatus.APPROVED);
        nurse.setRejectionReason(null);
        nurse.setRejectionDetail(null);
        return toResponse(nurseRepository.save(nurse));
    }

    @Override
    @Transactional
    public NurseResponse reject(UUID nurseId, NurseRejectionRequest request) {
        Nurse nurse = getNurseOrThrow(nurseId);

        NurseRejectionDetail detail = NurseRejectionDetail.builder()
                .nurse(nurse)
                .overallReason(request.getOverallReason())
                .failedSteps(request.getFailedSteps())
                .build();

        nurse.setVerificationStatus(VerificationStatus.REJECTED);
        nurse.setRejectionReason(request.getOverallReason());
        nurse.setRejectionDetail(detail);

        return toResponse(nurseRepository.save(nurse));
    }

    private Nurse getNurseOrThrow(UUID nurseId) {
        return nurseRepository.findById(nurseId)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse not found"));
    }

    private List<NurseServiceResponse> fetchServices(Nurse nurse) {
        return nurseServiceRepository.findAllByNurse_Id(nurse.getId()).stream()
                .map(nurseMapper::toServiceResponse)
                .sorted(Comparator.comparing(NurseServiceResponse::getServiceName))
                .toList();
    }

    private NurseResponse toResponse(Nurse nurse) {
        return nurseMapper.toResponse(nurse, fetchServices(nurse));
    }
}
