package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.NurseRejectionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NurseRejectionDetailRepository extends JpaRepository<NurseRejectionDetail, UUID> {

    Optional<NurseRejectionDetail> findByNurseId(UUID nurseId);

    void deleteByNurseId(UUID nurseId);
}
