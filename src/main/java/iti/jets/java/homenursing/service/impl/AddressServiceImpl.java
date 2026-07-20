package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.AddressRequest;
import iti.jets.java.homenursing.dto.AddressResponse;
import iti.jets.java.homenursing.entity.Address;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.AddressMapper;
import iti.jets.java.homenursing.repository.AddressRepository;
import iti.jets.java.homenursing.service.AddressService;
import iti.jets.java.homenursing.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final ProfileService profileService;

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getByProfileId(UUID profileId, UUID userId) {
        Profile profile = profileService.getOwnedProfileEntity(profileId, userId);
        Address address = addressRepository.findByProfileId(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found for profile: " + profileId));
        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse create(UUID profileId, UUID userId, AddressRequest request) {
        Profile profile = profileService.getOwnedProfileEntity(profileId, userId);

        addressRepository.findByProfileId(profile.getId()).ifPresent(existing -> {
            throw new BadRequestException("Address already exists for profile: " + profileId);
        });

        Address address = addressMapper.toEntity(request);
        address.setProfile(profile);

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse update(UUID profileId, UUID userId, AddressRequest request) {
        Profile profile = profileService.getOwnedProfileEntity(profileId, userId);
        Address address = addressRepository.findByProfileId(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found for profile: " + profileId));

        if (request.getCountry() != null) address.setCountry(request.getCountry());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getArea() != null) address.setArea(request.getArea());
        if (request.getStreet() != null) address.setStreet(request.getStreet());
        if (request.getBuildingNumber() != null) address.setBuildingNumber(request.getBuildingNumber());
        if (request.getApartmentNumber() != null) address.setApartmentNumber(request.getApartmentNumber());
        if (request.getLatitude() != null) address.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) address.setLongitude(request.getLongitude());

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID profileId, UUID userId) {
        Profile profile = profileService.getOwnedProfileEntity(profileId, userId);
        Address address = addressRepository.findByProfileId(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found for profile: " + profileId));
        addressRepository.delete(address);
    }
}
