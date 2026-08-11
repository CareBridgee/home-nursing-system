package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.profile.EmergencyContactRequest;
import iti.jets.java.homenursing.dto.profile.EmergencyContactResponse;
import iti.jets.java.homenursing.entity.EmergencyContact;
import iti.jets.java.homenursing.entity.Profile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class EmergencyContactMapperTest {

    private final EmergencyContactMapper mapper = Mappers.getMapper(EmergencyContactMapper.class);

    @Test
    void toEntity_mapsAllRequestFields() {
        EmergencyContactRequest request = EmergencyContactRequest.builder()
                .contactName("Omar Hassan")
                .relationship("Brother")
                .phoneNumber("+201012345678")
                .build();

        EmergencyContact entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getContactName()).isEqualTo("Omar Hassan");
        assertThat(entity.getRelationship()).isEqualTo("Brother");
        assertThat(entity.getPhoneNumber()).isEqualTo("+201012345678");
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
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 2, 14, 15);
        LocalDateTime updatedAt = createdAt.plusDays(1);
        Profile profile = Profile.builder().id(profileId).build();
        EmergencyContact contact = EmergencyContact.builder()
                .id(id)
                .profile(profile)
                .contactName("Sara Ali")
                .relationship("Mother")
                .phoneNumber("+2011555666777")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        EmergencyContactResponse response = mapper.toResponse(contact);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getProfileId()).isEqualTo(profileId);
        assertThat(response.getContactName()).isEqualTo("Sara Ali");
        assertThat(response.getRelationship()).isEqualTo("Mother");
        assertThat(response.getPhoneNumber()).isEqualTo("+2011555666777");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toResponse_nullProfile_yieldsNullProfileId() {
        EmergencyContact contact = EmergencyContact.builder().profile(null).build();

        assertThat(mapper.toResponse(contact).getProfileId()).isNull();
    }

    @Test
    void toResponse_nullContact_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
