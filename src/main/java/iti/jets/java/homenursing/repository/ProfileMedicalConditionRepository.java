package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.ProfileMedicalCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileMedicalConditionRepository extends JpaRepository<ProfileMedicalCondition, UUID> {

    List<ProfileMedicalCondition> findByProfileId(UUID profileId);

    boolean existsByProfileIdAndMedicalConditionId(UUID profileId, UUID medicalConditionId);

    Optional<ProfileMedicalCondition> findByProfileIdAndMedicalConditionId(UUID profileId, UUID medicalConditionId);
}
