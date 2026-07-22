package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.dto.notification.NotificationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationResponse create(NotificationRequest request);

    List<NotificationResponse> getMyNotifications(UUID userId);

    List<NotificationResponse> getMyNotificationsAfter(UUID userId, LocalDateTime after);

    NotificationResponse getNotification(UUID id, UUID userId);

    NotificationResponse markRead(UUID id, UUID userId);

    void delete(UUID id, UUID userId);
}
