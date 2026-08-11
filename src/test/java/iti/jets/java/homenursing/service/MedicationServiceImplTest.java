package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.catalog.MedicationRequest;
import iti.jets.java.homenursing.dto.catalog.MedicationResponse;
import iti.jets.java.homenursing.entity.Medication;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.MedicationMapper;
import iti.jets.java.homenursing.repository.MedicationRepository;
import iti.jets.java.homenursing.service.impl.MedicationServiceImpl;
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
class MedicationServiceImplTest {

    @Mock
    private MedicationRepository medicationRepository;
    @Mock
    private MedicationMapper medicationMapper;

    @InjectMocks
    private MedicationServiceImpl medicationService;

    private static final UUID MEDICATION_ID = UUID.randomUUID();

    private static Medication medication() {
        return Medication.builder().id(MEDICATION_ID).name("Metformin").build();
    }

    private static MedicationResponse response() {
        return new MedicationResponse(MEDICATION_ID, "Metformin", CatalogSource.ADMIN, null);
    }

    @Test
    void findAllMapsEveryMedication() {
        Medication first = medication();
        Medication second = Medication.builder().id(UUID.randomUUID()).name("Insulin").build();
        when(medicationRepository.findAll()).thenReturn(List.of(first, second));
        when(medicationMapper.toResponse(first)).thenReturn(response());
        when(medicationMapper.toResponse(second)).thenReturn(
                new MedicationResponse(second.getId(), "Insulin", CatalogSource.ADMIN, null));

        List<MedicationResponse> responses = medicationService.findAll();

        assertThat(responses).extracting(MedicationResponse::name).containsExactly("Metformin", "Insulin");
    }

    @Test
    void findAllBySourceDelegatesToRepository() {
        Medication medication = medication();
        when(medicationRepository.findBySource(CatalogSource.USER)).thenReturn(List.of(medication));
        when(medicationMapper.toResponse(medication)).thenReturn(response());

        List<MedicationResponse> responses = medicationService.findAll(CatalogSource.USER);

        assertThat(responses).singleElement().satisfies(r -> assertThat(r.name()).isEqualTo("Metformin"));
        verify(medicationRepository).findBySource(CatalogSource.USER);
    }

    @Test
    void getByIdReturnsMappedMedication() {
        Medication medication = medication();
        when(medicationRepository.findById(MEDICATION_ID)).thenReturn(Optional.of(medication));
        when(medicationMapper.toResponse(medication)).thenReturn(response());

        MedicationResponse result = medicationService.getById(MEDICATION_ID);

        assertThat(result.id()).isEqualTo(MEDICATION_ID);
    }

    @Test
    void getByIdWhenMissingThrows() {
        when(medicationRepository.findById(MEDICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicationService.getById(MEDICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medication not found");
    }

    @Test
    void createMapsEntityAndSaves() {
        MedicationRequest request = new MedicationRequest("Aspirin");
        Medication mapped = Medication.builder().name("Aspirin").build();
        Medication saved = medication();
        when(medicationMapper.toEntity(request)).thenReturn(mapped);
        when(medicationRepository.save(mapped)).thenReturn(saved);
        when(medicationMapper.toResponse(saved)).thenReturn(response());

        MedicationResponse result = medicationService.create(request);

        assertThat(result.name()).isEqualTo("Metformin");
        verify(medicationRepository).save(mapped);
    }

    @Test
    void updateSetsName() {
        Medication medication = medication();
        when(medicationRepository.findById(MEDICATION_ID)).thenReturn(Optional.of(medication));
        MedicationRequest request = new MedicationRequest("Ibuprofen");
        when(medicationRepository.save(medication)).thenReturn(medication);
        when(medicationMapper.toResponse(medication)).thenReturn(
                new MedicationResponse(MEDICATION_ID, "Ibuprofen", CatalogSource.ADMIN, null));

        MedicationResponse result = medicationService.update(MEDICATION_ID, request);

        assertThat(medication.getName()).isEqualTo("Ibuprofen");
        assertThat(result.name()).isEqualTo("Ibuprofen");
    }

    @Test
    void updateWhenMissingThrows() {
        when(medicationRepository.findById(MEDICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicationService.update(MEDICATION_ID, new MedicationRequest("X")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medication not found");

        verify(medicationRepository, never()).save(any(Medication.class));
    }

    @Test
    void deleteRemovesExistingMedication() {
        when(medicationRepository.existsById(MEDICATION_ID)).thenReturn(true);

        medicationService.delete(MEDICATION_ID);

        verify(medicationRepository).deleteById(MEDICATION_ID);
    }

    @Test
    void deleteWhenMissingThrows() {
        when(medicationRepository.existsById(MEDICATION_ID)).thenReturn(false);

        assertThatThrownBy(() -> medicationService.delete(MEDICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medication not found");

        verify(medicationRepository, never()).deleteById(any(UUID.class));
    }
}
