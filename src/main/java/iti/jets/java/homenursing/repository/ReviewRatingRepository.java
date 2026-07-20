package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.ReviewRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRatingRepository extends JpaRepository<ReviewRating, UUID> {

    List<ReviewRating> findByProfileId(UUID profileId);

    Page<ReviewRating> findByNurseId(UUID nurseId, Pageable pageable);

    Optional<ReviewRating> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);
}
