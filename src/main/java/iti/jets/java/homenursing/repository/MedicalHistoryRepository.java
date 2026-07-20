package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, UUID> {

    List<MedicalHistory> findByProfileIdOrderByCreatedAtDesc(UUID profileId);
}
