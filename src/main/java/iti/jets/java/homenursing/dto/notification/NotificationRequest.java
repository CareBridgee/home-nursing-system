package iti.jets.java.homenursing.dto.notification;

import iti.jets.java.homenursing.entity.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record NotificationRequest(
        UUID userId,
        @NotBlank String title,
        @NotBlank String message,
        NotificationType type,
        String relatedEntityType,
        UUID relatedEntityId
) {
}
