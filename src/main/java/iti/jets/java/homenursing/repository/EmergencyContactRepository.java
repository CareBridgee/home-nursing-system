package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {

    List<EmergencyContact> findByProfileId(UUID profileId);
}
