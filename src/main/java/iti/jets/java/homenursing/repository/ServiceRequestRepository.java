package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByProfile_User_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    List<ServiceRequest> findByProfile_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID profileId);

    Optional<ServiceRequest> findByIdAndIsDeletedFalse(UUID id);

    @Query("""
            SELECT COUNT(s) > 0 FROM ServiceRequest s
            WHERE s.id = :reservationId
              AND s.isDeleted = false
              AND (
                    s.profile.user.id = :userId
                 OR (s.nurse IS NOT NULL AND s.nurse.user.id = :userId)
                 OR EXISTS (
                      SELECT 1 FROM NurseOffer o
                      WHERE o.serviceRequest.id = :reservationId
                        AND o.isDeleted = false
                        AND o.nurse.user.id = :userId
                 )
              )
            """)
    boolean isParticipant(@Param("reservationId") UUID reservationId, @Param("userId") UUID userId);
}
