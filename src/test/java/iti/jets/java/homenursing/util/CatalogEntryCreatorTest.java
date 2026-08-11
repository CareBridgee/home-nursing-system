package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.entity.MedicalCondition;
import iti.jets.java.homenursing.entity.Medication;
import iti.jets.java.homenursing.entity.enums.AllergyType;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import iti.jets.java.homenursing.repository.AllergyRepository;
import iti.jets.java.homenursing.repository.MedicalConditionRepository;
import iti.jets.java.homenursing.repository.MedicationRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CatalogEntryCreatorTest {

    @Mock
    private AllergyRepository allergyRepository;
    @Mock
    private MedicationRepository medicationRepository;
    @Mock
    private MedicalConditionRepository medicalConditionRepository;

    @InjectMocks
    private CatalogEntryCreator creator;

    @Test
    void createAllergySavesUserSourceEntryWithGivenType() {
        Allergy saved = Allergy.builder().id(java.util.UUID.randomUUID()).build();
        when(allergyRepository.save(org.mockito.ArgumentMatchers.any(Allergy.class))).thenReturn(saved);

        Allergy result = creator.createAllergy("Penicillin", AllergyType.DRUG);

        assertThat(result).isSameAs(saved);
        ArgumentCaptor<Allergy> captor = ArgumentCaptor.forClass(Allergy.class);
        verify(allergyRepository).save(captor.capture());
        Allergy entry = captor.getValue();
        assertThat(entry.getName()).isEqualTo("Penicillin");
        assertThat(entry.getType()).isEqualTo(AllergyType.DRUG);
        assertThat(entry.getSource()).isEqualTo(CatalogSource.USER);
    }

    @Test
    void createAllergyDefaultsNullTypeToOther() {
        when(allergyRepository.save(org.mockito.ArgumentMatchers.any(Allergy.class)))
                .thenReturn(Allergy.builder().build());

        creator.createAllergy("Dust", null);

        ArgumentCaptor<Allergy> captor = ArgumentCaptor.forClass(Allergy.class);
        verify(allergyRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(AllergyType.OTHER);
        assertThat(captor.getValue().getSource()).isEqualTo(CatalogSource.USER);
    }

    @Test
    void createMedicationSavesUserSourceEntry() {
        when(medicationRepository.save(org.mockito.ArgumentMatchers.any(Medication.class)))
                .thenReturn(Medication.builder().build());

        creator.createMedication("Paracetamol");

        ArgumentCaptor<Medication> captor = ArgumentCaptor.forClass(Medication.class);
        verify(medicationRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Paracetamol");
        assertThat(captor.getValue().getSource()).isEqualTo(CatalogSource.USER);
    }

    @Test
    void createMedicalConditionSavesUserSourceEntryWithDescription() {
        when(medicalConditionRepository.save(org.mockito.ArgumentMatchers.any(MedicalCondition.class)))
                .thenReturn(MedicalCondition.builder().build());

        creator.createMedicalCondition("Diabetes", "Type 2");

        ArgumentCaptor<MedicalCondition> captor = ArgumentCaptor.forClass(MedicalCondition.class);
        verify(medicalConditionRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Diabetes");
        assertThat(captor.getValue().getDescription()).isEqualTo("Type 2");
        assertThat(captor.getValue().getSource()).isEqualTo(CatalogSource.USER);
    }

    @Test
    void createMedicalConditionAllowsNullDescription() {
        when(medicalConditionRepository.save(org.mockito.ArgumentMatchers.any(MedicalCondition.class)))
                .thenReturn(MedicalCondition.builder().build());

        creator.createMedicalCondition("Hypertension", null);

        ArgumentCaptor<MedicalCondition> captor = ArgumentCaptor.forClass(MedicalCondition.class);
        verify(medicalConditionRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isNull();
        assertThat(captor.getValue().getSource()).isEqualTo(CatalogSource.USER);
    }
}
