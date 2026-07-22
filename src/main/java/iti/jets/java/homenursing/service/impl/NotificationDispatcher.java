package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.notification.NotificationResponse;
import iti.jets.java.homenursing.entity.Notification;
import iti.jets.java.homenursing.mapper.NotificationMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationDispatcher {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationMapper notificationMapper;

    public NotificationDispatcher(SimpMessagingTemplate messagingTemplate,
                                  NotificationMapper notificationMapper) {
        this.messagingTemplate = messagingTemplate;
        this.notificationMapper = notificationMapper;
    }

    public void dispatch(Notification notification) {
        NotificationResponse payload = notificationMapper.toResponse(notification);
        String userId = notification.getUser().getId().toString();
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload);
    }
}
