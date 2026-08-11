package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.catalog.AllergyRequest;
import iti.jets.java.homenursing.dto.catalog.AllergyResponse;
import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.entity.enums.AllergyType;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.AllergyMapper;
import iti.jets.java.homenursing.repository.AllergyRepository;
import iti.jets.java.homenursing.service.impl.AllergyServiceImpl;
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
class AllergyServiceImplTest {

    @Mock
    private AllergyRepository allergyRepository;
    @Mock
    private AllergyMapper allergyMapper;

    @InjectMocks
    private AllergyServiceImpl allergyService;

    private static final UUID ALLERGY_ID = UUID.randomUUID();

    private static Allergy allergy() {
        return Allergy.builder().id(ALLERGY_ID).name("Peanuts").type(AllergyType.FOOD).build();
    }

    private static AllergyResponse response() {
        return new AllergyResponse(ALLERGY_ID, "Peanuts", AllergyType.FOOD, CatalogSource.ADMIN, null);
    }

    @Test
    void findAllMapsEveryAllergy() {
        Allergy first = allergy();
        Allergy second = Allergy.builder().id(UUID.randomUUID()).name("Pollen").type(AllergyType.OTHER).build();
        when(allergyRepository.findAll()).thenReturn(List.of(first, second));
        when(allergyMapper.toResponse(first)).thenReturn(response());
        when(allergyMapper.toResponse(second)).thenReturn(
                new AllergyResponse(second.getId(), "Pollen", AllergyType.OTHER, CatalogSource.ADMIN, null));

        List<AllergyResponse> responses = allergyService.findAll();

        assertThat(responses).extracting(AllergyResponse::name).containsExactly("Peanuts", "Pollen");
    }

    @Test
    void findAllBySourceDelegatesToRepository() {
        Allergy allergy = allergy();
        when(allergyRepository.findBySource(CatalogSource.USER)).thenReturn(List.of(allergy));
        when(allergyMapper.toResponse(allergy)).thenReturn(response());

        List<AllergyResponse> responses = allergyService.findAll(CatalogSource.USER);

        assertThat(responses).singleElement().satisfies(r -> assertThat(r.source()).isEqualTo(CatalogSource.ADMIN));
        verify(allergyRepository).findBySource(CatalogSource.USER);
    }

    @Test
    void getByIdReturnsMappedAllergy() {
        Allergy allergy = allergy();
        when(allergyRepository.findById(ALLERGY_ID)).thenReturn(Optional.of(allergy));
        when(allergyMapper.toResponse(allergy)).thenReturn(response());

        AllergyResponse result = allergyService.getById(ALLERGY_ID);

        assertThat(result.id()).isEqualTo(ALLERGY_ID);
    }

    @Test
    void getByIdWhenMissingThrows() {
        when(allergyRepository.findById(ALLERGY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> allergyService.getById(ALLERGY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Allergy not found");
    }

    @Test
    void createMapsEntityAndSaves() {
        AllergyRequest request = new AllergyRequest("Aspirin", AllergyType.DRUG);
        Allergy mapped = Allergy.builder().name("Aspirin").type(AllergyType.DRUG).build();
        Allergy saved = allergy();
        when(allergyMapper.toEntity(request)).thenReturn(mapped);
        when(allergyRepository.save(mapped)).thenReturn(saved);
        when(allergyMapper.toResponse(saved)).thenReturn(response());

        AllergyResponse result = allergyService.create(request);

        assertThat(result.name()).isEqualTo("Peanuts");
        verify(allergyRepository).save(mapped);
    }

    @Test
    void updateSetsNameAndType() {
        Allergy allergy = allergy();
        when(allergyRepository.findById(ALLERGY_ID)).thenReturn(Optional.of(allergy));
        AllergyRequest request = new AllergyRequest("Penicillin", AllergyType.DRUG);
        when(allergyRepository.save(allergy)).thenReturn(allergy);
        when(allergyMapper.toResponse(allergy)).thenReturn(
                new AllergyResponse(ALLERGY_ID, "Penicillin", AllergyType.DRUG, CatalogSource.ADMIN, null));

        AllergyResponse result = allergyService.update(ALLERGY_ID, request);

        assertThat(allergy.getName()).isEqualTo("Penicillin");
        assertThat(allergy.getType()).isEqualTo(AllergyType.DRUG);
        assertThat(result.name()).isEqualTo("Penicillin");
    }

    @Test
    void updateWhenMissingThrows() {
        when(allergyRepository.findById(ALLERGY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> allergyService.update(ALLERGY_ID, new AllergyRequest("X", AllergyType.OTHER)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Allergy not found");

        verify(allergyRepository, never()).save(any(Allergy.class));
    }

    @Test
    void deleteRemovesExistingAllergy() {
        when(allergyRepository.existsById(ALLERGY_ID)).thenReturn(true);

        allergyService.delete(ALLERGY_ID);

        verify(allergyRepository).deleteById(ALLERGY_ID);
    }

    @Test
    void deleteWhenMissingThrows() {
        when(allergyRepository.existsById(ALLERGY_ID)).thenReturn(false);

        assertThatThrownBy(() -> allergyService.delete(ALLERGY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Allergy not found");

        verify(allergyRepository, never()).deleteById(any(UUID.class));
    }
}
