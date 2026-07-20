package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.ProfileMedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileMedicationRepository extends JpaRepository<ProfileMedication, UUID> {

    List<ProfileMedication> findByProfileId(UUID profileId);

    boolean existsByProfileIdAndMedicationId(UUID profileId, UUID medicationId);

    Optional<ProfileMedication> findByProfileIdAndMedicationId(UUID profileId, UUID medicationId);
}
