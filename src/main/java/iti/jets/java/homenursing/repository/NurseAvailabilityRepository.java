package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.NurseAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NurseAvailabilityRepository extends JpaRepository<NurseAvailability, UUID> {

    List<NurseAvailability> findAllByNurse_Id(UUID nurseId);
}