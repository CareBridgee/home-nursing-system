package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.ProfileMedicalCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProfileMedicalConditionRepository extends JpaRepository<ProfileMedicalCondition, UUID> {
}
