package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.NurseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NurseServiceRepository extends JpaRepository<NurseService, UUID> {

    List<NurseService> findAllByNurse_Id(UUID nurseId);

    Optional<NurseService> findByNurse_IdAndServiceType_Id(UUID nurseId, UUID serviceTypeId);

    List<NurseService> findByServiceType_IdAndIsActiveTrue(UUID serviceTypeId);

    List<NurseService> findByServiceType_NameContainingIgnoreCaseAndIsActiveTrue(String serviceTypeName);
}
