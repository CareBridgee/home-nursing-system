package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.profile.AddressRequest;
import iti.jets.java.homenursing.dto.profile.AddressResponse;
import iti.jets.java.homenursing.entity.Address;
import iti.jets.java.homenursing.entity.Profile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class AddressMapperTest {

    private final AddressMapper mapper = Mappers.getMapper(AddressMapper.class);

    @Test
    void toEntity_mapsAllRequestFields() {
        AddressRequest request = AddressRequest.builder()
                .country("Egypt")
                .city("Cairo")
                .area("Nasr City")
                .street("Main St")
                .buildingNumber("12")
                .apartmentNumber("3B")
                .latitude(new BigDecimal("30.04440000"))
                .longitude(new BigDecimal("31.23570000"))
                .build();

        Address entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getCountry()).isEqualTo("Egypt");
        assertThat(entity.getCity()).isEqualTo("Cairo");
        assertThat(entity.getArea()).isEqualTo("Nasr City");
        assertThat(entity.getStreet()).isEqualTo("Main St");
        assertThat(entity.getBuildingNumber()).isEqualTo("12");
        assertThat(entity.getApartmentNumber()).isEqualTo("3B");
        assertThat(entity.getLatitude()).isEqualByComparingTo("30.04440000");
        assertThat(entity.getLongitude()).isEqualByComparingTo("31.23570000");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getProfile()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_nullRequest_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_mapsAllFieldsIncludingProfileId() {
        UUID profileId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 11, 10, 30);
        LocalDateTime updatedAt = createdAt.plusHours(2);
        Profile profile = Profile.builder().id(profileId).build();
        Address address = Address.builder()
                .id(id)
                .profile(profile)
                .country("Egypt")
                .city("Alexandria")
                .area("Smouha")
                .street("El Gaish")
                .buildingNumber("7")
                .apartmentNumber("2A")
                .latitude(new BigDecimal("31.20010000"))
                .longitude(new BigDecimal("29.91870000"))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        AddressResponse response = mapper.toResponse(address);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getProfileId()).isEqualTo(profileId);
        assertThat(response.getCountry()).isEqualTo("Egypt");
        assertThat(response.getCity()).isEqualTo("Alexandria");
        assertThat(response.getArea()).isEqualTo("Smouha");
        assertThat(response.getStreet()).isEqualTo("El Gaish");
        assertThat(response.getBuildingNumber()).isEqualTo("7");
        assertThat(response.getApartmentNumber()).isEqualTo("2A");
        assertThat(response.getLatitude()).isEqualByComparingTo("31.20010000");
        assertThat(response.getLongitude()).isEqualByComparingTo("29.91870000");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toResponse_nullProfile_yieldsNullProfileId() {
        Address address = Address.builder().profile(null).build();

        assertThat(mapper.toResponse(address).getProfileId()).isNull();
    }

    @Test
    void toResponse_nullAddress_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
