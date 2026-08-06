package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.Allergy;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AllergyRepository extends JpaRepository<Allergy, UUID> {

    Optional<Allergy> findByNameIgnoreCase(String name);

    List<Allergy> findBySource(CatalogSource source);
}