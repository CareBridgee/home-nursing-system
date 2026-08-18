package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.NurseOffer;
import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NurseOfferRepository extends JpaRepository<NurseOffer, UUID> {

    List<NurseOffer> findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID serviceRequestId);

    List<NurseOffer> findByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalse(
            UUID serviceRequestId, UUID userId);

    boolean existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalse(
            UUID serviceRequestId, UUID userId);

    Optional<NurseOffer> findByIdAndIsDeletedFalse(UUID id);

    Optional<NurseOffer> findByServiceRequest_IdAndStatusAndIsDeletedFalse(
            UUID serviceRequestId, NurseOfferStatus status);

    List<NurseOffer> findByServiceRequest_IdInAndStatusAndIsDeletedFalse(
            Collection<UUID> serviceRequestIds, NurseOfferStatus status);

    boolean existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalseAndStatus(
            UUID serviceRequestId, UUID userId, NurseOfferStatus status);
}
