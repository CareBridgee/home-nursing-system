package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferUpdateRequest;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseOffer;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.NurseOfferMapper;
import iti.jets.java.homenursing.repository.NurseOfferRepository;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.NurseServiceRepository;
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
    private final NurseServiceRepository nurseServiceRepository;
    private final NurseOfferMapper nurseOfferMapper;

    @Override
    @Transactional
    public NurseOfferResponse create(UUID userId, NurseOfferRequest request) {
        ServiceRequest serviceRequest = serviceRequestRepository.findByIdAndIsDeletedFalse(request.serviceRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + request.serviceRequestId()));
        Nurse nurse = nurseRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse profile not found"));

        if (serviceRequest.getStatus() != ServiceRequestStatus.SEARCHING || serviceRequest.getNurse() != null) {
            throw new BadRequestException("This service request is not accepting offers");
        }
        if (nurse.getVerificationStatus() != VerificationStatus.APPROVED
                || !Boolean.TRUE.equals(nurse.getIsAvailable())) {
            throw new BadRequestException("Nurse is not eligible to create offers");
        }
        boolean providesRequestedService = nurseServiceRepository
                .findByNurse_IdAndServiceType_Id(nurse.getId(), serviceRequest.getServiceType().getId())
                .map(nurseService -> Boolean.TRUE.equals(nurseService.getIsActive()))
                .orElse(false);
        if (!providesRequestedService) {
            throw new BadRequestException("Nurse does not provide the requested service");
        }
        if (nurseOfferRepository.existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalse(
                serviceRequest.getId(), userId)) {
            throw new BadRequestException("Nurse has already submitted an offer for this service request");
        }

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
    public NurseOfferResponse accept(UUID id, UUID userId) {
        NurseOffer offer = nurseOfferRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse offer not found: " + id));
        ServiceRequest serviceRequest = offer.getServiceRequest();

        if (!serviceRequest.getProfile().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Nurse offer not found: " + id);
        }
        if (offer.getStatus() != NurseOfferStatus.PENDING) {
            throw new BadRequestException("Only pending offers can be accepted");
        }
        if (serviceRequest.getNurse() != null) {
            throw new BadRequestException("A nurse has already been selected for this service request");
        }

        offer.setStatus(NurseOfferStatus.ACCEPTED);
        serviceRequest.setNurse(offer.getNurse());
        serviceRequest.setStatus(ServiceRequestStatus.ACCEPTED);

        nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(serviceRequest.getId())
                .stream()
                .filter(otherOffer -> !otherOffer.getId().equals(offer.getId()))
                .filter(otherOffer -> otherOffer.getStatus() == NurseOfferStatus.PENDING)
                .forEach(otherOffer -> otherOffer.setStatus(NurseOfferStatus.REJECTED));

        return nurseOfferMapper.toResponse(nurseOfferRepository.save(offer));
    }

    @Override
    @Transactional
    public NurseOfferResponse update(UUID id, UUID userId, NurseOfferUpdateRequest request) {
        NurseOffer offer = getOwnedOffer(id, userId);
        if (offer.getStatus() != NurseOfferStatus.PENDING) {
            throw new BadRequestException("Only pending offers can be updated");
        }
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
        return nurseOfferMapper.toResponse(nurseOfferRepository.save(offer));
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        NurseOffer offer = getOwnedOffer(id, userId);
        if (offer.getStatus() != NurseOfferStatus.PENDING) {
            throw new BadRequestException("Only pending offers can be deleted");
        }
        offer.setIsDeleted(true);
        nurseOfferRepository.save(offer);
    }

    private ServiceRequest getAuthorizedServiceRequest(UUID serviceRequestId, UUID userId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + serviceRequestId));
        boolean isOwner = serviceRequest.getProfile().getUser().getId().equals(userId);
        boolean isNurse = nurseRepository.existsByUser_Id(userId)
                && serviceRequest.getNurse() != null
                && serviceRequest.getNurse().getUser().getId().equals(userId);
        if (!isOwner && !isNurse) {
            throw new ResourceNotFoundException("Service request not found: " + serviceRequestId);
        }
        return serviceRequest;
    }

    private NurseOffer getAuthorizedOffer(UUID id, UUID userId) {
        NurseOffer offer = nurseOfferRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nurse offer not found: " + id));
        boolean isRequestOwner = offer.getServiceRequest().getProfile().getUser().getId().equals(userId);
        boolean isOfferingNurse = offer.getNurse().getUser().getId().equals(userId);
        if (!isRequestOwner && !isOfferingNurse) {
            throw new ResourceNotFoundException("Nurse offer not found: " + id);
        }
        return offer;
    }

    private NurseOffer getOwnedOffer(UUID id, UUID userId) {
        NurseOffer offer = getAuthorizedOffer(id, userId);
        if (!offer.getNurse().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Nurse offer not found: " + id);
        }
        return offer;
    }
}
