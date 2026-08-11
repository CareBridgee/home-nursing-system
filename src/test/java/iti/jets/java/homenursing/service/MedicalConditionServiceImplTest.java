package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.catalog.MedicalConditionRequest;
import iti.jets.java.homenursing.dto.catalog.MedicalConditionResponse;
import iti.jets.java.homenursing.entity.MedicalCondition;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.MedicalConditionMapper;
import iti.jets.java.homenursing.repository.MedicalConditionRepository;
import iti.jets.java.homenursing.service.impl.MedicalConditionServiceImpl;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class MedicalConditionServiceImplTest {

    @Mock
    private MedicalConditionRepository medicalConditionRepository;
    @Mock
    private MedicalConditionMapper medicalConditionMapper;

    @InjectMocks
    private MedicalConditionServiceImpl medicalConditionService;

    private static final UUID CONDITION_ID = UUID.randomUUID();

    private static MedicalCondition condition() {
        return MedicalCondition.builder().id(CONDITION_ID).name("Diabetes").description("Chronic").build();
    }

    private static MedicalConditionResponse response() {
        return new MedicalConditionResponse(CONDITION_ID, "Diabetes", "Chronic", CatalogSource.ADMIN, null);
    }

    @Test
    void findAllMapsEveryCondition() {
        MedicalCondition first = condition();
        MedicalCondition second = MedicalCondition.builder()
                .id(UUID.randomUUID()).name("Asthma").description("Lung").build();
        when(medicalConditionRepository.findAll()).thenReturn(List.of(first, second));
        when(medicalConditionMapper.toResponse(first)).thenReturn(response());
        when(medicalConditionMapper.toResponse(second)).thenReturn(
                new MedicalConditionResponse(second.getId(), "Asthma", "Lung", CatalogSource.ADMIN, null));

        List<MedicalConditionResponse> responses = medicalConditionService.findAll();

        assertThat(responses).extracting(MedicalConditionResponse::name).containsExactly("Diabetes", "Asthma");
    }

    @Test
    void findAllBySourceDelegatesToRepository() {
        MedicalCondition condition = condition();
        when(medicalConditionRepository.findBySource(CatalogSource.USER)).thenReturn(List.of(condition));
        when(medicalConditionMapper.toResponse(condition)).thenReturn(response());

        List<MedicalConditionResponse> responses = medicalConditionService.findAll(CatalogSource.USER);

        assertThat(responses).singleElement().satisfies(r -> assertThat(r.description()).isEqualTo("Chronic"));
        verify(medicalConditionRepository).findBySource(CatalogSource.USER);
    }

    @Test
    void getByIdReturnsMappedCondition() {
        MedicalCondition condition = condition();
        when(medicalConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(condition));
        when(medicalConditionMapper.toResponse(condition)).thenReturn(response());

        MedicalConditionResponse result = medicalConditionService.getById(CONDITION_ID);

        assertThat(result.id()).isEqualTo(CONDITION_ID);
    }

    @Test
    void getByIdWhenMissingThrows() {
        when(medicalConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalConditionService.getById(CONDITION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medical condition not found");
    }

    @Test
    void createMapsEntityAndSaves() {
        MedicalConditionRequest request = new MedicalConditionRequest("Migraine", "Recurring");
        MedicalCondition mapped = MedicalCondition.builder().name("Migraine").description("Recurring").build();
        MedicalCondition saved = condition();
        when(medicalConditionMapper.toEntity(request)).thenReturn(mapped);
        when(medicalConditionRepository.save(mapped)).thenReturn(saved);
        when(medicalConditionMapper.toResponse(saved)).thenReturn(response());

        MedicalConditionResponse result = medicalConditionService.create(request);

        assertThat(result.name()).isEqualTo("Diabetes");
        verify(medicalConditionRepository).save(mapped);
    }

    @Test
    void updateSetsNameAndDescription() {
        MedicalCondition condition = condition();
        when(medicalConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.of(condition));
        MedicalConditionRequest request = new MedicalConditionRequest("Hypertension", "High BP");
        when(medicalConditionRepository.save(condition)).thenReturn(condition);
        when(medicalConditionMapper.toResponse(condition)).thenReturn(
                new MedicalConditionResponse(CONDITION_ID, "Hypertension", "High BP", CatalogSource.ADMIN, null));

        MedicalConditionResponse result = medicalConditionService.update(CONDITION_ID, request);

        assertThat(condition.getName()).isEqualTo("Hypertension");
        assertThat(condition.getDescription()).isEqualTo("High BP");
        assertThat(result.name()).isEqualTo("Hypertension");
    }

    @Test
    void updateWhenMissingThrows() {
        when(medicalConditionRepository.findById(CONDITION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalConditionService.update(
                CONDITION_ID, new MedicalConditionRequest("X", null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medical condition not found");

        verify(medicalConditionRepository, never()).save(any(MedicalCondition.class));
    }

    @Test
    void deleteRemovesExistingCondition() {
        when(medicalConditionRepository.existsById(CONDITION_ID)).thenReturn(true);

        medicalConditionService.delete(CONDITION_ID);

        verify(medicalConditionRepository).deleteById(CONDITION_ID);
    }

    @Test
    void deleteWhenMissingThrows() {
        when(medicalConditionRepository.existsById(CONDITION_ID)).thenReturn(false);

        assertThatThrownBy(() -> medicalConditionService.delete(CONDITION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medical condition not found");

        verify(medicalConditionRepository, never()).deleteById(any(UUID.class));
    }
}
