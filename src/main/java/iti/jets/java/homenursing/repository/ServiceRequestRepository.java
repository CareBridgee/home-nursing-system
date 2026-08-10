package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByProfile_User_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    List<ServiceRequest> findByProfile_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID profileId);

    boolean existsByProfile_IdAndIsDeletedFalseAndStatusIn(UUID profileId, Collection<ServiceRequestStatus> statuses);

    List<ServiceRequest> findByProfile_User_IdAndIsDeletedFalseAndStatusInAndNurseNullOrderByCreatedAtDesc(
            UUID userId, Collection<ServiceRequestStatus> statuses);

    Optional<ServiceRequest> findFirstByNurse_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
            UUID nurseId, Collection<ServiceRequestStatus> statuses);

    Optional<ServiceRequest> findFirstByProfile_User_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
            UUID userId, Collection<ServiceRequestStatus> statuses);

    @EntityGraph(attributePaths = {"serviceType", "nurse.user"})
    List<ServiceRequest> findByProfile_User_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
            UUID userId, Collection<ServiceRequestStatus> statuses);

    Optional<ServiceRequest> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByNurse_IdAndIsDeletedFalseAndStatusIn(UUID nurseId, Collection<ServiceRequestStatus> statuses);

    boolean existsByProfile_IdAndNurse_User_IdAndIsDeletedFalseAndStatusIn(
            UUID profileId, UUID userId, Collection<ServiceRequestStatus> statuses);

    @EntityGraph(attributePaths = {"profile.user", "nurse.user", "serviceType"})
    Optional<ServiceRequest> findWithDetailsById(UUID id);

    @Query("""
            SELECT s FROM ServiceRequest s
            WHERE s.isDeleted = false
              AND s.nurse IS NULL
              AND s.serviceType.id IN :serviceTypeIds
              AND s.status IN :statuses
            ORDER BY s.createdAt DESC
            """)
    List<ServiceRequest> findOpenRequestsForServiceTypes(
            @Param("serviceTypeIds") List<UUID> serviceTypeIds,
            @Param("statuses") List<ServiceRequestStatus> statuses);

    @Query("""
            SELECT COUNT(s) > 0 FROM ServiceRequest s
            WHERE s.id = :reservationId
              AND s.isDeleted = false
              AND (
                    s.profile.user.id = :userId
                 OR EXISTS (
                      SELECT 1 FROM Nurse n
                      WHERE n.id = s.nurse.id AND n.user.id = :userId
                 )
                 OR EXISTS (
                      SELECT 1 FROM NurseOffer o
                      WHERE o.serviceRequest.id = :reservationId
                        AND o.isDeleted = false
                        AND o.status = 'PENDING'
                        AND o.nurse.user.id = :userId
                 )
              )
            """)
    boolean isParticipant(@Param("reservationId") UUID reservationId, @Param("userId") UUID userId);
}
