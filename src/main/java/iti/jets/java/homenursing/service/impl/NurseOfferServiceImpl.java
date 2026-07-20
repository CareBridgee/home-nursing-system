package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferUpdateRequest;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseOffer;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.NurseOfferMapper;
import iti.jets.java.homenursing.repository.NurseOfferRepository;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.service.NurseOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NurseOfferServiceImpl implements NurseOfferService {

    private final NurseOfferRepository nurseOfferRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final NurseRepository nurseRepository;
    private final NurseOfferMapper nurseOfferMapper;

    @Override
    @Transactional
    public NurseOfferResponse create(UUID userId, NurseOfferRequest request) {
        ServiceRequest serviceRequest = getAuthorizedServiceRequest(request.serviceRequestId(), userId);
        Nurse nurse = nurseRepository.findById(request.nurseId())
                .orElseThrow(() -> new ResourceNotFoundException("Nurse not found: " + request.nurseId()));
        NurseOffer offer = nurseOfferMapper.toEntity(request);
        offer.setServiceRequest(serviceRequest);
        offer.setNurse(nurse);
        offer.setStatus(NurseOfferStatus.PENDING);
        offer.setIsDeleted(false);
        return nurseOfferMapper.toResponse(nurseOfferRepository.save(offer));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NurseOfferResponse> listByServiceRequest(UUID serviceRequestId, UUID userId) {
        getAuthorizedServiceRequest(serviceRequestId, userId);
        return nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(serviceRequestId).stream()
                .map(nurseOfferMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NurseOfferResponse get(UUID id, UUID userId) {
        return nurseOfferMapper.toResponse(getAuthorizedOffer(id, userId));
    }

    @Override
    @Transactional
    public NurseOfferResponse update(UUID id, UUID userId, NurseOfferUpdateRequest request) {
        NurseOffer offer = getAuthorizedOffer(id, userId);
        if (request.proposedPrice() != null) {
            offer.setProposedPrice(request.proposedPrice());
        }
        if (request.proposedDate() != null) {
            offer.setProposedDate(request.proposedDate());
        }
        if (request.proposedTime() != null) {
            offer.setProposedTime(request.proposedTime());
        }
        if (request.message() != null) {
            offer.setMessage(request.message());
        }
        if (request.status() != null) {
            offer.setStatus(request.status());
        }
        return nurseOfferMapper.toResponse(nurseOfferRepository.save(offer));
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        NurseOffer offer = getAuthorizedOffer(id, userId);
        offer.setIsDeleted(true);
        nurseOfferRepository.save(offer);
    }

    private ServiceRequest getAuthorizedServiceRequest(UUID serviceRequestId, UUID userId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findByIdAndIsDeletedFalse(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + serviceRequestId));
        boolean isOwner = serviceRequest.getProfile().getUser().getId().equals(userId);
        boolean isNurse = nurseRepository.findByUser_Id(userId)
                .map(n -> n.getId().equals(serviceRequest.getNurse() != null
                        ? serviceRequest.getNurse().getId() : null))
                .orElse(false);
        if (!isOwner && !isNurse) {
            throw new ResourceNotFoundException("Service request not found: " + serviceRequestId);
        }
        return serviceRequest;
    }

    private NurseOffer getAuthorizedOffer(UUID id, UUID userId) {
        NurseOffer offer = nurseOfferRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse offer not found: " + id));
        getAuthorizedServiceRequest(offer.getServiceRequest().getId(), userId);
        return offer;
    }
}
