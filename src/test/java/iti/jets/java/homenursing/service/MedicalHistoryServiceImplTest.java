package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.profile.MedicalHistoryRequest;
import iti.jets.java.homenursing.dto.profile.MedicalHistoryResponse;
import iti.jets.java.homenursing.entity.MedicalHistory;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.MedicalHistoryType;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.MedicalHistoryMapper;
import iti.jets.java.homenursing.repository.MedicalHistoryRepository;
import iti.jets.java.homenursing.service.impl.MedicalHistoryServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class MedicalHistoryServiceImplTest {

    @Mock
    private MedicalHistoryRepository medicalHistoryRepository;
    @Mock
    private MedicalHistoryMapper medicalHistoryMapper;
    @Mock
    private ProfileService profileService;

    @InjectMocks
    private MedicalHistoryServiceImpl medicalHistoryService;

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID HISTORY_ID = UUID.randomUUID();

    private static MedicalHistory history(UUID ownerId) {
        return MedicalHistory.builder()
                .id(HISTORY_ID)
                .profile(Profile.builder().id(PROFILE_ID).user(User.builder().id(ownerId).build()).build())
                .type(MedicalHistoryType.SURGERY)
                .description("Appendectomy 2010")
                .build();
    }

    private static MedicalHistoryResponse response(UUID ownerId) {
        return MedicalHistoryResponse.builder()
                .id(HISTORY_ID)
                .profileId(PROFILE_ID)
                .type(MedicalHistoryType.SURGERY)
                .description("Appendectomy 2010")
                .build();
    }

    @Test
    void listByProfileWithOwnerVerifiesOwnershipAndMaps() {
        MedicalHistory history = history(USER_ID);
        when(medicalHistoryRepository.findByProfileIdOrderByCreatedAtDesc(PROFILE_ID)).thenReturn(List.of(history));
        when(medicalHistoryMapper.toResponse(history)).thenReturn(response(USER_ID));

        List<MedicalHistoryResponse> responses = medicalHistoryService.listByProfile(PROFILE_ID, USER_ID);

        verify(profileService).getOwnedProfileEntity(PROFILE_ID, USER_ID);
        assertThat(responses).singleElement().satisfies(r -> assertThat(r.getDescription()).isEqualTo("Appendectomy 2010"));
    }

    @Test
    void listByProfileWithoutOwnershipCheckVerifiesExistence() {
        MedicalHistory history = history(USER_ID);
        when(profileService.getProfile(PROFILE_ID)).thenReturn(Profile.builder().id(PROFILE_ID).build());
        when(medicalHistoryRepository.findByProfileIdOrderByCreatedAtDesc(PROFILE_ID)).thenReturn(List.of(history));
        when(medicalHistoryMapper.toResponse(history)).thenReturn(response(USER_ID));

        List<MedicalHistoryResponse> responses = medicalHistoryService.listByProfile(PROFILE_ID);

        assertThat(responses).hasSize(1);
    }

    @Test
    void getByIdReturnsMappedHistory() {
        MedicalHistory history = history(USER_ID);
        when(medicalHistoryRepository.findById(HISTORY_ID)).thenReturn(Optional.of(history));
        when(medicalHistoryMapper.toResponse(history)).thenReturn(response(USER_ID));

        MedicalHistoryResponse result = medicalHistoryService.getById(HISTORY_ID, USER_ID);

        assertThat(result.getId()).isEqualTo(HISTORY_ID);
    }

    @Test
    void getByIdWhenNotOwnerThrows() {
        MedicalHistory history = history(UUID.randomUUID());
        when(medicalHistoryRepository.findById(HISTORY_ID)).thenReturn(Optional.of(history));

        assertThatThrownBy(() -> medicalHistoryService.getById(HISTORY_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medical history not found");
    }

    @Test
    void getByIdWhenMissingThrows() {
        when(medicalHistoryRepository.findById(HISTORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalHistoryService.getById(HISTORY_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medical history not found");
    }

    @Test
    void createBuildsEntityWithProfileAndSaves() {
        Profile profile = Profile.builder().id(PROFILE_ID).build();
        when(profileService.getOwnedProfileEntity(PROFILE_ID, USER_ID)).thenReturn(profile);
        MedicalHistoryRequest request = MedicalHistoryRequest.builder()
                .type(MedicalHistoryType.PROCEDURE)
                .description("Colonoscopy")
                .build();
        MedicalHistory mapped = MedicalHistory.builder().type(MedicalHistoryType.PROCEDURE).build();
        when(medicalHistoryMapper.toEntity(request)).thenReturn(mapped);
        when(medicalHistoryRepository.save(any(MedicalHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(medicalHistoryMapper.toResponse(any(MedicalHistory.class))).thenReturn(response(USER_ID));

        MedicalHistoryResponse result = medicalHistoryService.create(PROFILE_ID, USER_ID, request);

        assertThat(mapped.getProfile()).isSameAs(profile);
        assertThat(result.getId()).isEqualTo(HISTORY_ID);
        verify(medicalHistoryRepository).save(mapped);
    }

    @Test
    void updateAppliesProvidedFields() {
        MedicalHistory history = history(USER_ID);
        when(medicalHistoryRepository.findById(HISTORY_ID)).thenReturn(Optional.of(history));
        MedicalHistoryRequest request = MedicalHistoryRequest.builder()
                .type(MedicalHistoryType.HOSPITALIZATION)
                .description("Pneumonia 2020")
                .build();
        when(medicalHistoryRepository.save(any(MedicalHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(medicalHistoryMapper.toResponse(any(MedicalHistory.class))).thenReturn(response(USER_ID));

        medicalHistoryService.update(HISTORY_ID, USER_ID, request);

        assertThat(history.getType()).isEqualTo(MedicalHistoryType.HOSPITALIZATION);
        assertThat(history.getDescription()).isEqualTo("Pneumonia 2020");
    }

    @Test
    void updateWithNullFieldsLeavesEntityUntouched() {
        MedicalHistory history = history(USER_ID);
        when(medicalHistoryRepository.findById(HISTORY_ID)).thenReturn(Optional.of(history));
        when(medicalHistoryRepository.save(any(MedicalHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(medicalHistoryMapper.toResponse(any(MedicalHistory.class))).thenReturn(response(USER_ID));

        medicalHistoryService.update(HISTORY_ID, USER_ID, MedicalHistoryRequest.builder().build());

        assertThat(history.getType()).isEqualTo(MedicalHistoryType.SURGERY);
        assertThat(history.getDescription()).isEqualTo("Appendectomy 2010");
    }

    @Test
    void updateWhenNotOwnerThrows() {
        MedicalHistory history = history(UUID.randomUUID());
        when(medicalHistoryRepository.findById(HISTORY_ID)).thenReturn(Optional.of(history));

        assertThatThrownBy(() -> medicalHistoryService.update(
                HISTORY_ID, USER_ID, MedicalHistoryRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medical history not found");
    }

    @Test
    void deleteRemovesOwnedHistory() {
        MedicalHistory history = history(USER_ID);
        when(medicalHistoryRepository.findById(HISTORY_ID)).thenReturn(Optional.of(history));

        medicalHistoryService.delete(HISTORY_ID, USER_ID);

        verify(medicalHistoryRepository).delete(history);
    }

    @Test
    void deleteWhenMissingThrows() {
        when(medicalHistoryRepository.findById(HISTORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalHistoryService.delete(HISTORY_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medical history not found");
    }
}
