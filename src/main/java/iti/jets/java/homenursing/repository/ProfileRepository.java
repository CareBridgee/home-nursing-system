package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    List<Profile> findByUserId(UUID userId);

    Optional<Profile> findByUserIdAndRelationshipIsNull(UUID userId);
}
