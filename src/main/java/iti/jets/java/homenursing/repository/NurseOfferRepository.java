package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.NurseOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NurseOfferRepository extends JpaRepository<NurseOffer, UUID> {

    List<NurseOffer> findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID serviceRequestId);

    Optional<NurseOffer> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalse(UUID serviceRequestId, UUID userId);
}
