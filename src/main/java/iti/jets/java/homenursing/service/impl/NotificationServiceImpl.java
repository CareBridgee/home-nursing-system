package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.dto.notification.NotificationResponse;
import iti.jets.java.homenursing.entity.Notification;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.NotificationMapper;
import iti.jets.java.homenursing.repository.NotificationRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationDispatcher notificationDispatcher;

    @Override
    @Transactional
    public NotificationResponse create(NotificationRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));
        Notification notification = notificationMapper.toEntity(request);
        notification.setUser(user);
        notification.setIsRead(false);
        Notification saved = notificationRepository.save(notification);
        notificationDispatcher.dispatch(saved);
        return notificationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(UUID userId) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotificationsAfter(UUID userId, LocalDateTime after) {
        return notificationRepository.findByUser_IdAndCreatedAtAfterOrderByCreatedAtAsc(userId, after).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotification(UUID id, UUID userId) {
        return notificationMapper.toResponse(getAuthorizedNotification(id, userId));
    }

    @Override
    @Transactional
    public NotificationResponse markRead(UUID id, UUID userId) {
        Notification notification = getAuthorizedNotification(id, userId);
        notification.setIsRead(true);
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        Notification notification = getAuthorizedNotification(id, userId);
        notificationRepository.delete(notification);
    }

    private Notification getAuthorizedNotification(UUID id, UUID userId) {
        return notificationRepository.findByUser_IdAndId(userId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
    }
}
