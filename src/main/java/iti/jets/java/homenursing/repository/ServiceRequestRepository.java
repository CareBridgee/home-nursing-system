package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByProfile_User_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    List<ServiceRequest> findByProfile_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID profileId);

    Optional<ServiceRequest> findByIdAndIsDeletedFalse(UUID id);
}
