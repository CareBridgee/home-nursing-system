package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.dto.notification.NotificationResponse;
import iti.jets.java.homenursing.entity.Notification;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.NotificationType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class NotificationMapperTest {

    private final NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);

    @Test
    void toEntity_mapsAllRequestFields() {
        UUID relatedEntityId = UUID.randomUUID();
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(),
                "New booking",
                "A nurse accepted your request",
                NotificationType.BOOKING,
                "SERVICE_REQUEST",
                relatedEntityId
        );

        Notification entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getTitle()).isEqualTo("New booking");
        assertThat(entity.getMessage()).isEqualTo("A nurse accepted your request");
        assertThat(entity.getType()).isEqualTo(NotificationType.BOOKING);
        assertThat(entity.getRelatedEntityType()).isEqualTo("SERVICE_REQUEST");
        assertThat(entity.getRelatedEntityId()).isEqualTo(relatedEntityId);
        assertThat(entity.getId()).isNull();
        assertThat(entity.getUser()).isNull();
        assertThat(entity.getIsRead()).isFalse();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_nullRequest_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_mapsAllFieldsIncludingUserId() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        UUID relatedEntityId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 20, 10);
        LocalDateTime updatedAt = createdAt.plusMinutes(5);
        User user = User.builder().id(userId).build();
        Notification notification = Notification.builder()
                .id(id)
                .user(user)
                .title("Reminder")
                .message("Your visit starts soon")
                .type(NotificationType.REMINDER)
                .isRead(false)
                .relatedEntityType("SERVICE_REQUEST")
                .relatedEntityId(relatedEntityId)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        NotificationResponse response = mapper.toResponse(notification);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.title()).isEqualTo("Reminder");
        assertThat(response.message()).isEqualTo("Your visit starts soon");
        assertThat(response.type()).isEqualTo(NotificationType.REMINDER);
        assertThat(response.isRead()).isFalse();
        assertThat(response.relatedEntityType()).isEqualTo("SERVICE_REQUEST");
        assertThat(response.relatedEntityId()).isEqualTo(relatedEntityId);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toResponse_nullUser_yieldsNullUserId() {
        Notification notification = Notification.builder().user(null).build();

        assertThat(mapper.toResponse(notification).userId()).isNull();
    }

    @Test
    void toResponse_nullNotification_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
