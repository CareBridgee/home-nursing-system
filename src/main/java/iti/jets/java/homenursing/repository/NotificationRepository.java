package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<Notification> findByUser_IdAndId(UUID userId, UUID id);
}
