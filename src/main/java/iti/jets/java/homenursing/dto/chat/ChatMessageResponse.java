package iti.jets.java.homenursing.dto.chat;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID serviceRequestId,
        UUID senderUserId,
        String senderName,
        String senderPhone,
        String content,
        LocalDateTime createdAt
) {
}
