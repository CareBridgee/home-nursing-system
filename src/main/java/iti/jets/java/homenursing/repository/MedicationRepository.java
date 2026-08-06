package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.Medication;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, UUID> {

    Optional<Medication> findByNameIgnoreCase(String name);

    List<Medication> findBySource(CatalogSource source);
}