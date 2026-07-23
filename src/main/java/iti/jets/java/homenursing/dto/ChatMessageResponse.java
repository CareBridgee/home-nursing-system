package iti.jets.java.homenursing.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID serviceRequestId,
        UUID senderUserId,
        String content,
        LocalDateTime createdAt
) {
}
