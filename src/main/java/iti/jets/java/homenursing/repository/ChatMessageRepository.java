package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.ChatMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    @EntityGraph(attributePaths = "serviceRequest")
    List<ChatMessage> findByServiceRequest_IdAndCreatedAtAfterOrderByCreatedAtAsc(UUID serviceRequestId, LocalDateTime after);

    @EntityGraph(attributePaths = "serviceRequest")
    List<ChatMessage> findByServiceRequest_IdOrderByCreatedAtAsc(UUID serviceRequestId);
}
