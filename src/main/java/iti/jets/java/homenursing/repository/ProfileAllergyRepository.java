package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.ProfileAllergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileAllergyRepository extends JpaRepository<ProfileAllergy, UUID> {

    List<ProfileAllergy> findByProfileId(UUID profileId);

    boolean existsByProfileIdAndAllergyId(UUID profileId, UUID allergyId);

    Optional<ProfileAllergy> findByProfileIdAndAllergyId(UUID profileId, UUID allergyId);
}
