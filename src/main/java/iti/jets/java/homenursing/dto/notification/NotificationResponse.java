package iti.jets.java.homenursing.dto.notification;

import iti.jets.java.homenursing.entity.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String title,
        String message,
        NotificationType type,
        Boolean isRead,
        String relatedEntityType,
        UUID relatedEntityId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
