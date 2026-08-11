package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.profile.MedicalHistoryResponse;
import iti.jets.java.homenursing.dto.profile.ProfileAllergyResponse;
import iti.jets.java.homenursing.dto.profile.ProfileMedicalConditionResponse;
import iti.jets.java.homenursing.dto.profile.ProfileMedicationResponse;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.AllergyType;
import iti.jets.java.homenursing.entity.enums.MedicalHistoryType;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.impl.ProfileReportServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ProfileReportServiceImplTest {

    @Mock
    private ChatClient reportChatClient;
    @Mock
    private ProfileService profileService;
    @Mock
    private MedicalHistoryService medicalHistoryService;
    @Mock
    private ProfileMedicalConditionService profileMedicalConditionService;
    @Mock
    private ProfileAllergyService profileAllergyService;
    @Mock
    private ProfileMedicationService profileMedicationService;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private ProfileReportServiceImpl profileReportService;

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    private static Profile profile(UUID ownerId) {
        return Profile.builder()
                .id(PROFILE_ID)
                .user(User.builder().id(ownerId).build())
                .firstName("Sara")
                .lastName("Mohamed")
                .build();
    }

    @Test
    void generateReportForOwnerBuildsContextAndCallsChat() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(OWNER_ID);
            when(profileService.getProfile(PROFILE_ID)).thenReturn(profile(OWNER_ID));
            when(medicalHistoryService.listByProfile(PROFILE_ID)).thenReturn(List.of(
                    new MedicalHistoryResponse(UUID.randomUUID(), PROFILE_ID, MedicalHistoryType.SURGERY,
                            "Appendectomy", LocalDateTime.now(), LocalDateTime.now())));
            when(profileMedicalConditionService.listByProfile(PROFILE_ID)).thenReturn(List.of(
                    new ProfileMedicalConditionResponse(UUID.randomUUID(), PROFILE_ID, UUID.randomUUID(),
                            "Diabetes", LocalDateTime.now())));
            when(profileAllergyService.listByProfile(PROFILE_ID)).thenReturn(List.of(
                    new ProfileAllergyResponse(UUID.randomUUID(), PROFILE_ID, UUID.randomUUID(),
                            "Peanuts", AllergyType.FOOD, LocalDateTime.now())));
            when(profileMedicationService.listByProfile(PROFILE_ID)).thenReturn(List.of(
                    new ProfileMedicationResponse(UUID.randomUUID(), PROFILE_ID, UUID.randomUUID(),
                            "Metformin", LocalDateTime.now())));

            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
            when(reportChatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.system(anyString())).thenReturn(requestSpec);
            when(requestSpec.user(anyString())).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callSpec);
            when(callSpec.content()).thenReturn("Generated report");

            String report = profileReportService.generateReport(PROFILE_ID);

            assertThat(report).isEqualTo("Generated report");
            verify(serviceRequestRepository, never())
                    .existsByProfile_IdAndNurse_User_IdAndIsDeletedFalse(any(), any());

            ArgumentCaptor<String> userContext = ArgumentCaptor.forClass(String.class);
            verify(requestSpec).user(userContext.capture());
            assertThat(userContext.getValue())
                    .contains("PATIENT PROFILE:")
                    .contains("Name: Sara Mohamed")
                    .contains("MEDICAL HISTORY:")
                    .contains("MEDICAL CONDITIONS:")
                    .contains("ALLERGIES:")
                    .contains("MEDICATIONS:")
                    .contains("MEDICAL HISTORY:\n- ")
                    .contains("Peanuts");
        }
    }

    @Test
    void generateReportForAssignedNursePasses() {
        UUID nurseUserId = UUID.randomUUID();
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(nurseUserId);
            when(profileService.getProfile(PROFILE_ID)).thenReturn(profile(OWNER_ID));
            when(medicalHistoryService.listByProfile(PROFILE_ID)).thenReturn(List.of());
            when(profileMedicalConditionService.listByProfile(PROFILE_ID)).thenReturn(List.of());
            when(profileAllergyService.listByProfile(PROFILE_ID)).thenReturn(List.of());
            when(profileMedicationService.listByProfile(PROFILE_ID)).thenReturn(List.of());
            when(serviceRequestRepository.existsByProfile_IdAndNurse_User_IdAndIsDeletedFalse(
                    PROFILE_ID, nurseUserId)).thenReturn(true);

            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
            when(reportChatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.system(anyString())).thenReturn(requestSpec);
            when(requestSpec.user(anyString())).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callSpec);
            when(callSpec.content()).thenReturn("Nurse report");

            String report = profileReportService.generateReport(PROFILE_ID);

            assertThat(report).isEqualTo("Nurse report");
            verify(serviceRequestRepository).existsByProfile_IdAndNurse_User_IdAndIsDeletedFalse(
                    PROFILE_ID, nurseUserId);
        }
    }

    @Test
    void generateReportForUnrelatedUserThrows() {
        UUID stranger = UUID.randomUUID();
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(stranger);
            when(profileService.getProfile(PROFILE_ID)).thenReturn(profile(OWNER_ID));
            when(serviceRequestRepository.existsByProfile_IdAndNurse_User_IdAndIsDeletedFalse(
                    PROFILE_ID, stranger)).thenReturn(false);

            assertThatThrownBy(() -> profileReportService.generateReport(PROFILE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Profile not found");
        }
    }
}
