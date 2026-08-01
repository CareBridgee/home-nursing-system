package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.ChatMessageResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ChatMessageService {

    ChatMessageResponse sendMessage(UUID reservationId, UUID senderUserId, String content);

    List<ChatMessageResponse> getMessages(UUID reservationId, UUID userId, LocalDateTime after);
}
