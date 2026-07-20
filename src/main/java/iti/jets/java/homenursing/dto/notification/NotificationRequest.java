package iti.jets.java.homenursing.dto.notification;

import iti.jets.java.homenursing.entity.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NotificationRequest(
        @NotNull UUID userId,
        @NotBlank String title,
        @NotBlank String message,
        NotificationType type,
        String relatedEntityType,
        UUID relatedEntityId
) {
}
