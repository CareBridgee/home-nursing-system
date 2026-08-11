package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.entity.MedicalCondition;
import iti.jets.java.homenursing.entity.Medication;
import iti.jets.java.homenursing.entity.enums.AllergyType;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import iti.jets.java.homenursing.repository.AllergyRepository;
import iti.jets.java.homenursing.repository.MedicalConditionRepository;
import iti.jets.java.homenursing.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogEntryCreator {

    private final AllergyRepository allergyRepository;
    private final MedicationRepository medicationRepository;
    private final MedicalConditionRepository medicalConditionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Allergy createAllergy(String name, AllergyType type) {
        return allergyRepository.save(Allergy.builder()
                .name(name)
                .type(type == null ? AllergyType.OTHER : type)
                .source(CatalogSource.USER)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Medication createMedication(String name) {
        return medicationRepository.save(Medication.builder()
                .name(name)
                .source(CatalogSource.USER)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MedicalCondition createMedicalCondition(String name, String description) {
        return medicalConditionRepository.save(MedicalCondition.builder()
                .name(name)
                .description(description)
                .source(CatalogSource.USER)
                .build());
    }
}
