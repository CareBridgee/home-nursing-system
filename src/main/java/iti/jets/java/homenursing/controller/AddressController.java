package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.AddressRequest;
import iti.jets.java.homenursing.dto.AddressResponse;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles/{profileId}/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<AddressResponse> getAddress(@PathVariable UUID profileId) {
        return ResponseEntity.ok(addressService.getByProfileId(profileId, SecurityUtils.currentUserId()));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(@PathVariable UUID profileId,
                                                         @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.create(profileId, SecurityUtils.currentUserId(), request));
    }

    @PutMapping
    public ResponseEntity<AddressResponse> updateAddress(@PathVariable UUID profileId,
                                                         @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(profileId, SecurityUtils.currentUserId(), request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID profileId) {
        addressService.delete(profileId, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
