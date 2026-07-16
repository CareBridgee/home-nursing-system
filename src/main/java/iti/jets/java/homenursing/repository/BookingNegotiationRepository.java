package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.BookingNegotiation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingNegotiationRepository extends JpaRepository<BookingNegotiation, UUID> {
}