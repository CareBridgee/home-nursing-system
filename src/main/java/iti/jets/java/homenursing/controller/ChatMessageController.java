package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.ChatMessageResponse;
import iti.jets.java.homenursing.dto.chat.SendMessageRequest;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.ChatMessageService;
import jakarta.validation.Valid;
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

    private final ChatMessageService chatMessageService;

    public ChatMessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable UUID reservationId,
            @RequestParam(value = "after", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<LocalDateTime> after) {
        List<ChatMessageResponse> messages = chatMessageService
                .getMessages(reservationId, SecurityUtils.currentUserId(), after.orElse(null));
        return ResponseEntity.ok(messages);
    }

    @PostMapping
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable UUID reservationId,
            @Valid @RequestBody SendMessageRequest request) {
        ChatMessageResponse response = chatMessageService
                .sendMessage(reservationId, SecurityUtils.currentUserId(), request.content());
        return ResponseEntity.ok(response);
    }

}
