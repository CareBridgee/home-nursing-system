package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.ChatMessageResponse;
import iti.jets.java.homenursing.repository.ChatMessageRepository;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.impl.ReservationParticipantService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations/{reservationId}/messages")
public class ChatMessageController {

    private final ChatMessageRepository chatMessageRepository;
    private final ReservationParticipantService participantService;

    public ChatMessageController(ChatMessageRepository chatMessageRepository,
                                  ReservationParticipantService participantService) {
        this.chatMessageRepository = chatMessageRepository;
        this.participantService = participantService;
    }

    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable UUID reservationId,
            @RequestParam("after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<LocalDateTime> after) {
        UUID userId = SecurityUtils.currentUserId();
        if (!participantService.isParticipant(reservationId, userId)) {
            return ResponseEntity.status(403).build();
        }
        List<ChatMessageResponse> messages;
        if (after.isPresent()) {
            messages = chatMessageRepository
                    .findByServiceRequest_IdAndCreatedAtAfterOrderByCreatedAtAsc(reservationId, after.get())
                    .stream()
                    .map(msg -> new ChatMessageResponse(msg.getId(), msg.getServiceRequest().getId(), msg.getSenderUserId(), msg.getContent(), msg.getCreatedAt()))
                    .toList();
        } else {
            messages = chatMessageRepository
                    .findByServiceRequest_IdOrderByCreatedAtAsc(reservationId)
                    .stream()
                    .map(msg -> new ChatMessageResponse(msg.getId(), msg.getServiceRequest().getId(), msg.getSenderUserId(), msg.getContent(), msg.getCreatedAt()))
                    .toList();
        }
        return ResponseEntity.ok(messages);
    }

}
