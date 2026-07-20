package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.AddressRequest;
import iti.jets.java.homenursing.dto.AddressResponse;

import java.util.UUID;

public interface AddressService {

    AddressResponse getByProfileId(UUID profileId, UUID userId);

    AddressResponse create(UUID profileId, UUID userId, AddressRequest request);

    AddressResponse update(UUID profileId, UUID userId, AddressRequest request);

    void delete(UUID profileId, UUID userId);
}
