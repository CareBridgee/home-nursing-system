package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.profile.EmergencyContactRequest;
import iti.jets.java.homenursing.dto.profile.EmergencyContactResponse;
import iti.jets.java.homenursing.entity.EmergencyContact;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.DuplicateResourceException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.EmergencyContactMapper;
import iti.jets.java.homenursing.repository.EmergencyContactRepository;
import iti.jets.java.homenursing.service.impl.EmergencyContactServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
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
class EmergencyContactServiceImplTest {

    @Mock
    private EmergencyContactRepository emergencyContactRepository;
    @Mock
    private EmergencyContactMapper emergencyContactMapper;
    @Mock
    private ProfileService profileService;

    @InjectMocks
    private EmergencyContactServiceImpl emergencyContactService;

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();

    private static EmergencyContact contact(UUID ownerId) {
        return EmergencyContact.builder()
                .id(CONTACT_ID)
                .profile(Profile.builder().id(PROFILE_ID).user(User.builder().id(ownerId).build()).build())
                .contactName("Ali")
                .relationship("Brother")
                .phoneNumber("+201001112223")
                .build();
    }

    private static EmergencyContactResponse response() {
        return EmergencyContactResponse.builder()
                .id(CONTACT_ID)
                .profileId(PROFILE_ID)
                .contactName("Ali")
                .relationship("Brother")
                .phoneNumber("+201001112223")
                .build();
    }

    @Test
    void listByProfileVerifiesOwnershipAndMaps() {
        EmergencyContact contact = contact(USER_ID);
        when(emergencyContactRepository.findByProfileId(PROFILE_ID)).thenReturn(List.of(contact));
        when(emergencyContactMapper.toResponse(contact)).thenReturn(response());

        List<EmergencyContactResponse> responses = emergencyContactService.listByProfile(PROFILE_ID, USER_ID);

        verify(profileService).getOwnedProfileEntity(PROFILE_ID, USER_ID);
        assertThat(responses).singleElement().satisfies(r -> assertThat(r.getContactName()).isEqualTo("Ali"));
    }

    @Test
    void getByIdReturnsMappedContact() {
        EmergencyContact contact = contact(USER_ID);
        when(emergencyContactRepository.findById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(emergencyContactMapper.toResponse(contact)).thenReturn(response());

        EmergencyContactResponse result = emergencyContactService.getById(CONTACT_ID, USER_ID);

        assertThat(result.getId()).isEqualTo(CONTACT_ID);
    }

    @Test
    void getByIdWhenNotOwnerThrows() {
        EmergencyContact contact = contact(UUID.randomUUID());
        when(emergencyContactRepository.findById(CONTACT_ID)).thenReturn(Optional.of(contact));

        assertThatThrownBy(() -> emergencyContactService.getById(CONTACT_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Emergency contact not found");
    }

    @Test
    void getByIdWhenMissingThrows() {
        when(emergencyContactRepository.findById(CONTACT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emergencyContactService.getById(CONTACT_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Emergency contact not found");
    }

    @Test
    void createWithDuplicatePhoneThrows() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(emergencyContactRepository.existsByProfile_IdAndPhoneNumber(PROFILE_ID, "+201001112223"))
                .thenReturn(true);

        assertThatThrownBy(() -> emergencyContactService.create(PROFILE_ID, USER_ID,
                EmergencyContactRequest.builder().contactName("Ali").phoneNumber("+201001112223").build()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createNormalizesPhoneAndSaves() {
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(emergencyContactRepository.existsByProfile_IdAndPhoneNumber(PROFILE_ID, "+201001112223"))
                .thenReturn(false);
        EmergencyContactRequest request = EmergencyContactRequest.builder()
                .contactName("Ali")
                .relationship("Brother")
                .phoneNumber("+20 (100) 111-2223")
                .build();
        EmergencyContact mapped = EmergencyContact.builder().contactName("Ali").relationship("Brother").build();
        when(emergencyContactMapper.toEntity(request)).thenReturn(mapped);
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emergencyContactMapper.toResponse(any(EmergencyContact.class))).thenReturn(response());

        EmergencyContactResponse result = emergencyContactService.create(PROFILE_ID, USER_ID, request);

        assertThat(mapped.getProfile()).isNotNull();
        assertThat(mapped.getPhoneNumber()).isEqualTo("+201001112223");
        assertThat(result.getId()).isEqualTo(CONTACT_ID);
    }

    @Test
    void updateAppliesFieldsAndNormalizedPhone() {
        EmergencyContact contact = contact(USER_ID);
        when(emergencyContactRepository.findById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(emergencyContactRepository.existsByProfile_IdAndPhoneNumberAndIdNot(
                PROFILE_ID, "+201001112223", CONTACT_ID)).thenReturn(false);
        EmergencyContactRequest request = EmergencyContactRequest.builder()
                .contactName("Omar")
                .relationship("Cousin")
                .phoneNumber("+20 (100) 111-2223")
                .build();
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emergencyContactMapper.toResponse(any(EmergencyContact.class))).thenReturn(response());

        emergencyContactService.update(CONTACT_ID, USER_ID, request);

        assertThat(contact.getContactName()).isEqualTo("Omar");
        assertThat(contact.getRelationship()).isEqualTo("Cousin");
        assertThat(contact.getPhoneNumber()).isEqualTo("+201001112223");
    }

    @Test
    void updateWithDuplicatePhoneThrows() {
        EmergencyContact contact = contact(USER_ID);
        when(emergencyContactRepository.findById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(emergencyContactRepository.existsByProfile_IdAndPhoneNumberAndIdNot(
                PROFILE_ID, "+201001112223", CONTACT_ID)).thenReturn(true);

        assertThatThrownBy(() -> emergencyContactService.update(CONTACT_ID, USER_ID,
                EmergencyContactRequest.builder().phoneNumber("+20 (100) 111-2223").build()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void updateWithNullPhoneLeavesPhoneUntouched() {
        EmergencyContact contact = contact(USER_ID);
        when(emergencyContactRepository.findById(CONTACT_ID)).thenReturn(Optional.of(contact));
        EmergencyContactRequest request = EmergencyContactRequest.builder().contactName("Omar").build();
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emergencyContactMapper.toResponse(any(EmergencyContact.class))).thenReturn(response());

        emergencyContactService.update(CONTACT_ID, USER_ID, request);

        assertThat(contact.getContactName()).isEqualTo("Omar");
        assertThat(contact.getPhoneNumber()).isEqualTo("+201001112223");
        verify(emergencyContactRepository, never())
                .existsByProfile_IdAndPhoneNumberAndIdNot(any(), any(), any());
    }

    @Test
    void deleteRemovesOwnedContact() {
        EmergencyContact contact = contact(USER_ID);
        when(emergencyContactRepository.findById(CONTACT_ID)).thenReturn(Optional.of(contact));

        emergencyContactService.delete(CONTACT_ID, USER_ID);

        verify(emergencyContactRepository).delete(contact);
    }

    @Test
    void deleteWhenMissingThrows() {
        when(emergencyContactRepository.findById(CONTACT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emergencyContactService.delete(CONTACT_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Emergency contact not found");
    }

    @Test
    void normalizePhoneNumberKeepsPlusAndStripsNonDigits() {
        String normalized = ReflectionTestUtils.invokeMethod(
                emergencyContactService, "normalizePhoneNumber", "+20 (123) 456-7890");

        assertThat(normalized).isEqualTo("+201234567890");
    }

    @Test
    void normalizePhoneNumberWithoutPlusReturnsDigitsOnly() {
        String normalized = ReflectionTestUtils.invokeMethod(
                emergencyContactService, "normalizePhoneNumber", "0100 123 4567");

        assertThat(normalized).isEqualTo("01001234567");
    }

    @Test
    void normalizePhoneNumberWithNullThrows() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                emergencyContactService, "normalizePhoneNumber", new Object[]{null}))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    void normalizePhoneNumberWithBlankThrows() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                emergencyContactService, "normalizePhoneNumber", "   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    void normalizePhoneNumberWithTooFewDigitsThrows() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                emergencyContactService, "normalizePhoneNumber", "+12345"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    void normalizePhoneNumberWithTooManyDigitsThrows() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                emergencyContactService, "normalizePhoneNumber", "+123456789012345678901"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid phone number format");
    }
}
