package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.dto.notification.NotificationResponse;
import iti.jets.java.homenursing.entity.Notification;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.NotificationType;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.NotificationMapper;
import iti.jets.java.homenursing.repository.NotificationRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.impl.NotificationServiceImpl;
import iti.jets.java.homenursing.util.AfterCommitExecutor;
import iti.jets.java.homenursing.util.NotificationDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private NotificationDispatcher notificationDispatcher;
    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    private NotificationServiceImpl service;

    private static final UUID NOTIF_ID = UUID.randomUUID();
    private static final UUID NOTIF_ID_2 = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RELATED_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 10, 0);

    private static final NotificationResponse RESPONSE = new NotificationResponse(
            NOTIF_ID, USER_ID, "Title", "Message", NotificationType.BOOKING, false,
            "SERVICE_REQUEST", RELATED_ID, NOW, NOW);

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(
                notificationRepository, userRepository, notificationMapper,
                notificationDispatcher, afterCommitExecutor);
    }

    private void runAfterCommitEvents() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(afterCommitExecutor).execute(any(Runnable.class));
    }

    private User user() {
        return User.builder().id(USER_ID).firstName("Mona").build();
    }

    private Notification notification() {
        return Notification.builder()
                .id(NOTIF_ID)
                .user(user())
                .title("Title")
                .message("Message")
                .type(NotificationType.BOOKING)
                .isRead(false)
                .relatedEntityType("SERVICE_REQUEST")
                .relatedEntityId(RELATED_ID)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private NotificationRequest request() {
        return new NotificationRequest(
                USER_ID, "Title", "Message", NotificationType.BOOKING, "SERVICE_REQUEST", RELATED_ID);
    }

    @Test
    void create_happy_savesAndDispatchesAfterCommit() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(notification());
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(RESPONSE);
        runAfterCommitEvents();

        NotificationResponse response = service.create(request());

        assertEquals(RESPONSE, response);
        verify(userRepository).findById(USER_ID);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationDispatcher).dispatch(captor.capture());
        assertEquals(USER_ID, captor.getValue().getUser().getId());
        assertFalse(Boolean.TRUE.equals(captor.getValue().getIsRead()));
    }

    @Test
    void create_nullUserId_throws() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.create(new NotificationRequest(
                        null, "Title", "Message", NotificationType.BOOKING, "SERVICE_REQUEST", RELATED_ID)));

        assertEquals("userId is required", ex.getMessage());
    }

    @Test
    void create_userNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                service.create(request()));

        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    void getMyNotifications_mapsAll() {
        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(notification(), notification()));
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(RESPONSE);

        List<NotificationResponse> result = service.getMyNotifications(USER_ID);

        assertEquals(2, result.size());
        assertEquals(List.of(RESPONSE, RESPONSE), result);
    }

    @Test
    void getMyNotifications_empty_returnsEmpty() {
        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of());

        List<NotificationResponse> result = service.getMyNotifications(USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void getMyNotificationsAfter_mapsAll() {
        when(notificationRepository.findByUser_IdAndCreatedAtAfterOrderByCreatedAtAsc(USER_ID, NOW))
                .thenReturn(List.of(notification()));
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(RESPONSE);

        List<NotificationResponse> result = service.getMyNotificationsAfter(USER_ID, NOW);

        assertEquals(1, result.size());
        assertEquals(RESPONSE, result.get(0));
    }

    @Test
    void getNotification_returnsMapped() {
        when(notificationRepository.findByUser_IdAndId(USER_ID, NOTIF_ID))
                .thenReturn(Optional.of(notification()));
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(RESPONSE);

        NotificationResponse response = service.getNotification(NOTIF_ID, USER_ID);

        assertEquals(RESPONSE, response);
    }

    @Test
    void getNotification_notFound_throws() {
        when(notificationRepository.findByUser_IdAndId(USER_ID, NOTIF_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                service.getNotification(NOTIF_ID, USER_ID));

        assertTrue(ex.getMessage().contains("Notification not found"));
    }

    @Test
    void markRead_marksAndSaves() {
        Notification unread = notification();
        when(notificationRepository.findByUser_IdAndId(USER_ID, NOTIF_ID)).thenReturn(Optional.of(unread));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(RESPONSE);

        NotificationResponse response = service.markRead(NOTIF_ID, USER_ID);

        assertEquals(RESPONSE, response);
        assertTrue(Boolean.TRUE.equals(unread.getIsRead()));
        verify(notificationRepository).save(unread);
    }

    @Test
    void delete_removesNotification() {
        Notification existing = notification();
        when(notificationRepository.findByUser_IdAndId(USER_ID, NOTIF_ID)).thenReturn(Optional.of(existing));

        service.delete(NOTIF_ID, USER_ID);

        verify(notificationRepository).delete(existing);
    }

    @Test
    void delete_notFound_throws() {
        when(notificationRepository.findByUser_IdAndId(USER_ID, NOTIF_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(NOTIF_ID, USER_ID));
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void getNotification_idAssignedToAnotherUser_throws() {
        when(notificationRepository.findByUser_IdAndId(eq(USER_ID), eq(NOTIF_ID_2))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getNotification(NOTIF_ID_2, USER_ID));
    }
}