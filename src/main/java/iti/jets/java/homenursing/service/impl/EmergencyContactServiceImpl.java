package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.EmergencyContactRequest;
import iti.jets.java.homenursing.dto.EmergencyContactResponse;
import iti.jets.java.homenursing.entity.EmergencyContact;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.EmergencyContactMapper;
import iti.jets.java.homenursing.repository.EmergencyContactRepository;
import iti.jets.java.homenursing.service.EmergencyContactService;
import iti.jets.java.homenursing.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmergencyContactServiceImpl implements EmergencyContactService {

    private final EmergencyContactRepository emergencyContactRepository;
    private final EmergencyContactMapper emergencyContactMapper;
    private final ProfileService profileService;

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> listByProfile(UUID profileId, UUID userId) {
        profileService.getOwnedProfileEntity(profileId, userId);
        return emergencyContactRepository.findByProfileId(profileId)
                .stream()
                .map(emergencyContactMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmergencyContactResponse getById(UUID id, UUID userId) {
        EmergencyContact contact = loadOwned(id, userId);
        return emergencyContactMapper.toResponse(contact);
    }

    @Override
    @Transactional
    public EmergencyContactResponse create(UUID profileId, UUID userId, EmergencyContactRequest request) {
        Profile profile = profileService.getOwnedProfileEntity(profileId, userId);
        EmergencyContact contact = emergencyContactMapper.toEntity(request);
        contact.setProfile(profile);
        EmergencyContact saved = emergencyContactRepository.save(contact);
        return emergencyContactMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public EmergencyContactResponse update(UUID id, UUID userId, EmergencyContactRequest request) {
        EmergencyContact contact = loadOwned(id, userId);
        if (request.getContactName() != null) contact.setContactName(request.getContactName());
        if (request.getRelationship() != null) contact.setRelationship(request.getRelationship());
        if (request.getPhoneNumber() != null) contact.setPhoneNumber(request.getPhoneNumber());
        EmergencyContact saved = emergencyContactRepository.save(contact);
        return emergencyContactMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        EmergencyContact contact = loadOwned(id, userId);
        emergencyContactRepository.delete(contact);
    }

    private EmergencyContact loadOwned(UUID id, UUID userId) {
        EmergencyContact contact = emergencyContactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency contact not found: " + id));
        if (!contact.getProfile().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Emergency contact not found: " + id);
        }
        return contact;
    }
}
