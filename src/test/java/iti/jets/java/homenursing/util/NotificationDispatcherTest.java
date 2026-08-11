package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.dto.notification.NotificationResponse;
import iti.jets.java.homenursing.entity.Notification;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.mapper.NotificationMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationDispatcher dispatcher;

    @Test
    void dispatchesMappedPayloadToUsersNotificationQueue() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        Notification notification = Notification.builder().id(UUID.randomUUID()).user(user).build();
        NotificationResponse payload = new NotificationResponse(
                notification.getId(), userId, "title", "message", null, false,
                null, null, null, null);
        when(notificationMapper.toResponse(notification)).thenReturn(payload);

        dispatcher.dispatch(notification);

        verify(notificationMapper).toResponse(notification);
        verify(messagingTemplate).convertAndSendToUser(
                userId.toString(), "/queue/notifications", payload);
    }
}
