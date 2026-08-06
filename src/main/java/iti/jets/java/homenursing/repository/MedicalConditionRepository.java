package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.MedicalCondition;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalConditionRepository extends JpaRepository<MedicalCondition, UUID> {

    Optional<MedicalCondition> findByNameIgnoreCase(String name);

    List<MedicalCondition> findBySource(CatalogSource source);
}