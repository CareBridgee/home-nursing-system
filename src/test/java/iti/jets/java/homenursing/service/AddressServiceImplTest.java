package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.profile.AddressRequest;
import iti.jets.java.homenursing.dto.profile.AddressResponse;
import iti.jets.java.homenursing.entity.Address;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.AddressMapper;
import iti.jets.java.homenursing.repository.AddressRepository;
import iti.jets.java.homenursing.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private ProfileService profileService;

    @InjectMocks
    private AddressServiceImpl addressService;

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();

    private static Address address() {
        return Address.builder()
                .id(ADDRESS_ID)
                .profile(Profile.builder().id(PROFILE_ID).build())
                .country("Egypt")
                .city("Giza")
                .build();
    }

    private static AddressResponse response() {
        return AddressResponse.builder()
                .id(ADDRESS_ID)
                .profileId(PROFILE_ID)
                .country("Egypt")
                .city("Giza")
                .build();
    }

    private static Profile profile() {
        return Profile.builder().id(PROFILE_ID).build();
    }

    @Test
    void getByProfileIdReturnsMappedAddress() {
        Address address = address();
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(profile());
        when(addressRepository.findByProfileId(PROFILE_ID)).thenReturn(Optional.of(address));
        when(addressMapper.toResponse(address)).thenReturn(response());

        AddressResponse result = addressService.getByProfileId(PROFILE_ID, USER_ID);

        assertThat(result.getId()).isEqualTo(ADDRESS_ID);
    }

    @Test
    void getByProfileIdWhenMissingThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(profile());
        when(addressRepository.findByProfileId(PROFILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getByProfileId(PROFILE_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found for profile");
    }

    @Test
    void createWhenAddressAlreadyExistsThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(profile());
        when(addressRepository.findByProfileId(PROFILE_ID)).thenReturn(Optional.of(address()));

        assertThatThrownBy(() -> addressService.create(PROFILE_ID, USER_ID, AddressRequest.builder().build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Address already exists");
    }

    @Test
    void createBuildsEntityWithProfileAndSaves() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(profile());
        when(addressRepository.findByProfileId(PROFILE_ID)).thenReturn(Optional.empty());
        AddressRequest request = AddressRequest.builder().city("Cairo").country("Egypt").build();
        Address mapped = Address.builder().city("Cairo").country("Egypt").build();
        when(addressMapper.toEntity(request)).thenReturn(mapped);
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
        when(addressMapper.toResponse(any(Address.class))).thenReturn(response());

        AddressResponse result = addressService.create(PROFILE_ID, USER_ID, request);

        assertThat(mapped.getProfile()).isNotNull();
        assertThat(result.getId()).isEqualTo(ADDRESS_ID);
        verify(addressRepository).save(mapped);
    }

    @Test
    void updateAppliesAllProvidedFields() {
        Address address = address();
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(profile());
        when(addressRepository.findByProfileId(PROFILE_ID)).thenReturn(Optional.of(address));
        AddressRequest request = AddressRequest.builder()
                .country("Egypt")
                .city("Alexandria")
                .area("Smouha")
                .street("Corniche")
                .buildingNumber("12")
                .apartmentNumber("3B")
                .latitude(new BigDecimal("31.2"))
                .longitude(new BigDecimal("29.9"))
                .build();
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
        when(addressMapper.toResponse(any(Address.class))).thenReturn(response());

        addressService.update(PROFILE_ID, USER_ID, request);

        assertThat(address.getCountry()).isEqualTo("Egypt");
        assertThat(address.getCity()).isEqualTo("Alexandria");
        assertThat(address.getArea()).isEqualTo("Smouha");
        assertThat(address.getStreet()).isEqualTo("Corniche");
        assertThat(address.getBuildingNumber()).isEqualTo("12");
        assertThat(address.getApartmentNumber()).isEqualTo("3B");
        assertThat(address.getLatitude()).isEqualByComparingTo("31.2");
        assertThat(address.getLongitude()).isEqualByComparingTo("29.9");
        verify(addressRepository).save(address);
    }

    @Test
    void updateWithNullFieldsLeavesEntityUntouched() {
        Address address = address();
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(profile());
        when(addressRepository.findByProfileId(PROFILE_ID)).thenReturn(Optional.of(address));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
        when(addressMapper.toResponse(any(Address.class))).thenReturn(response());

        addressService.update(PROFILE_ID, USER_ID, AddressRequest.builder().build());

        assertThat(address.getCity()).isEqualTo("Giza");
        verify(addressRepository).save(address);
    }

    @Test
    void updateWhenMissingThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(profile());
        when(addressRepository.findByProfileId(PROFILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.update(PROFILE_ID, USER_ID, AddressRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found for profile");
    }

    @Test
    void deleteRemovesExistingAddress() {
        Address address = address();
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(profile());
        when(addressRepository.findByProfileId(PROFILE_ID)).thenReturn(Optional.of(address));

        addressService.delete(PROFILE_ID, USER_ID);

        verify(addressRepository).delete(address);
    }

    @Test
    void deleteWhenMissingThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(profile());
        when(addressRepository.findByProfileId(PROFILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.delete(PROFILE_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found for profile");

        verify(addressRepository, never()).delete(any(Address.class));
    }
}
