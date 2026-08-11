package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.chat.ChatMessageResponse;
import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.entity.ChatMessage;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.NotificationType;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ForbiddenException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.repository.ChatMessageRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.impl.ChatMessageServiceImpl;
import iti.jets.java.homenursing.util.AfterCommitExecutor;
import iti.jets.java.homenursing.util.ReservationParticipantHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMessageServiceImplTest {

    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID PATIENT_USER_ID = UUID.randomUUID();
    private static final UUID NURSE_USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReservationParticipantHelper participantHelper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    private ChatMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatMessageServiceImpl(
                chatMessageRepository,
                serviceRequestRepository,
                userRepository,
                participantHelper,
                notificationService,
                messagingTemplate,
                afterCommitExecutor);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(afterCommitExecutor).execute(any(Runnable.class));
        when(participantHelper.isParticipant(eq(REQUEST_ID), any(UUID.class))).thenReturn(true);
    }

    private User patientUser() {
        return User.builder().id(PATIENT_USER_ID).firstName("Mona").lastName("Ali").phoneNumber("01000").build();
    }

    private User nurseUser() {
        return User.builder().id(NURSE_USER_ID).firstName("Sara").lastName("Hany").phoneNumber("01111").build();
    }

    private ServiceRequest request() {
        return ServiceRequest.builder()
                .id(REQUEST_ID)
                .profile(Profile.builder().id(PROFILE_ID).user(patientUser()).build())
                .nurse(Nurse.builder().id(UUID.randomUUID()).user(nurseUser()).build())
                .build();
    }

    private ServiceRequest requestWithoutNurse() {
        return ServiceRequest.builder()
                .id(REQUEST_ID)
                .profile(Profile.builder().id(PROFILE_ID).user(patientUser()).build())
                .build();
    }

    private ChatMessage message(UUID senderId, String content) {
        return ChatMessage.builder()
                .id(UUID.randomUUID())
                .serviceRequest(request())
                .senderUserId(senderId)
                .content(content)
                .createdAt(LocalDateTime.of(2026, 8, 12, 10, 0))
                .build();
    }

    @Test
    void sendMessage_notParticipant_throwsForbidden() {
        when(participantHelper.isParticipant(REQUEST_ID, PATIENT_USER_ID)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> service.sendMessage(REQUEST_ID, PATIENT_USER_ID, "Hello"));

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void sendMessage_nullContent_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> service.sendMessage(REQUEST_ID, PATIENT_USER_ID, null));
    }

    @Test
    void sendMessage_blankContent_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> service.sendMessage(REQUEST_ID, PATIENT_USER_ID, "   "));
    }

    @Test
    void sendMessage_reservationMissing_throwsNotFound() {
        when(serviceRequestRepository.findWithDetailsById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.sendMessage(REQUEST_ID, PATIENT_USER_ID, "Hello"));

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void sendMessage_patientSender_savesPublishesAndNotifiesNurseOnly() {
        ServiceRequest sr = request();
        when(serviceRequestRepository.findWithDetailsById(REQUEST_ID)).thenReturn(Optional.of(sr));
        when(userRepository.findById(PATIENT_USER_ID)).thenReturn(Optional.of(patientUser()));

        ChatMessageResponse response = service.sendMessage(REQUEST_ID, PATIENT_USER_ID, "Hello nurse");

        assertEquals(REQUEST_ID, response.serviceRequestId());
        assertEquals(PATIENT_USER_ID, response.senderUserId());
        assertEquals("Mona Ali", response.senderName());
        assertEquals("01000", response.senderPhone());
        assertEquals("Hello nurse", response.content());

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        assertEquals(sr, messageCaptor.getValue().getServiceRequest());
        assertEquals(PATIENT_USER_ID, messageCaptor.getValue().getSenderUserId());
        assertEquals("Hello nurse", messageCaptor.getValue().getContent());

        verify(messagingTemplate).convertAndSend("/topic/chat/" + REQUEST_ID, response);

        verify(notificationService).create(new NotificationRequest(
                NURSE_USER_ID,
                "New Message",
                "You have a new message for this reservation.",
                NotificationType.MESSAGE,
                "SERVICE_REQUEST",
                REQUEST_ID));
        verify(notificationService, never()).create(new NotificationRequest(
                PATIENT_USER_ID,
                "New Message",
                "You have a new message for this reservation.",
                NotificationType.MESSAGE,
                "SERVICE_REQUEST",
                REQUEST_ID));
    }

    @Test
    void sendMessage_nurseSender_notifiesPatientOnly() {
        when(serviceRequestRepository.findWithDetailsById(REQUEST_ID)).thenReturn(Optional.of(request()));
        when(userRepository.findById(NURSE_USER_ID)).thenReturn(Optional.of(nurseUser()));

        ChatMessageResponse response = service.sendMessage(REQUEST_ID, NURSE_USER_ID, "Hi Mona");

        assertEquals("Sara Hany", response.senderName());
        assertEquals("01111", response.senderPhone());
        verify(notificationService).create(new NotificationRequest(
                PATIENT_USER_ID,
                "New Message",
                "You have a new message for this reservation.",
                NotificationType.MESSAGE,
                "SERVICE_REQUEST",
                REQUEST_ID));
        verify(notificationService, never()).create(new NotificationRequest(
                NURSE_USER_ID,
                "New Message",
                "You have a new message for this reservation.",
                NotificationType.MESSAGE,
                "SERVICE_REQUEST",
                REQUEST_ID));
    }

    @Test
    void sendMessage_noNurseAssigned_notifiesNobody() {
        when(serviceRequestRepository.findWithDetailsById(REQUEST_ID)).thenReturn(Optional.of(requestWithoutNurse()));
        when(userRepository.findById(PATIENT_USER_ID)).thenReturn(Optional.of(patientUser()));

        service.sendMessage(REQUEST_ID, PATIENT_USER_ID, "Hello");

        verify(notificationService, never()).create(any(NotificationRequest.class));
    }

    @Test
    void sendMessage_senderNotFound_returnsNullSenderFields() {
        when(serviceRequestRepository.findWithDetailsById(REQUEST_ID)).thenReturn(Optional.of(request()));
        when(userRepository.findById(PATIENT_USER_ID)).thenReturn(Optional.empty());

        ChatMessageResponse response = service.sendMessage(REQUEST_ID, PATIENT_USER_ID, "Hello");

        assertNull(response.senderName());
        assertNull(response.senderPhone());
        verify(notificationService).create(new NotificationRequest(
                NURSE_USER_ID,
                "New Message",
                "You have a new message for this reservation.",
                NotificationType.MESSAGE,
                "SERVICE_REQUEST",
                REQUEST_ID));
    }

    @Test
    void getMessages_notParticipant_throwsForbidden() {
        when(participantHelper.isParticipant(REQUEST_ID, OTHER_USER_ID)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> service.getMessages(REQUEST_ID, OTHER_USER_ID, null));
    }

    @Test
    void getMessages_withAfter_queriesTimestampAndResolvesSenders() {
        LocalDateTime after = LocalDateTime.of(2026, 8, 12, 9, 0);
        ChatMessage fromPatient = message(PATIENT_USER_ID, "Hello");
        ChatMessage fromUnknown = message(OTHER_USER_ID, "Who?");
        when(chatMessageRepository.findByServiceRequest_IdAndCreatedAtAfterOrderByCreatedAtAsc(REQUEST_ID, after))
                .thenReturn(List.of(fromPatient, fromUnknown));
        when(userRepository.findAllById(anySet())).thenReturn(List.of(patientUser()));

        List<ChatMessageResponse> responses = service.getMessages(REQUEST_ID, PATIENT_USER_ID, after);

        assertEquals(2, responses.size());
        assertEquals("Mona Ali", responses.get(0).senderName());
        assertEquals("Hello", responses.get(0).content());
        assertNull(responses.get(1).senderName());
        assertEquals("Who?", responses.get(1).content());
    }

    @Test
    void getMessages_noAfter_fetchesFullHistory() {
        when(chatMessageRepository.findByServiceRequest_IdOrderByCreatedAtAsc(REQUEST_ID))
                .thenReturn(List.of(message(PATIENT_USER_ID, "Hi")));
        when(userRepository.findAllById(anySet())).thenReturn(List.of(patientUser()));

        List<ChatMessageResponse> responses = service.getMessages(REQUEST_ID, PATIENT_USER_ID, null);

        assertEquals(1, responses.size());
        assertEquals("Mona Ali", responses.get(0).senderName());
        verify(chatMessageRepository, never())
                .findByServiceRequest_IdAndCreatedAtAfterOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void getMessages_emptyHistory_returnsEmptyList() {
        when(chatMessageRepository.findByServiceRequest_IdOrderByCreatedAtAsc(REQUEST_ID)).thenReturn(List.of());

        List<ChatMessageResponse> responses = service.getMessages(REQUEST_ID, PATIENT_USER_ID, null);

        assertTrue(responses.isEmpty());
        verify(userRepository, never()).findAllById(anySet());
    }

    @Test
    void getMessages_fetchesAllMessagesForAnyRequest() {
        UUID otherRequest = UUID.randomUUID();
        ChatMessage msg = ChatMessage.builder()
                .id(UUID.randomUUID())
                .serviceRequest(ServiceRequest.builder().id(otherRequest).build())
                .senderUserId(PATIENT_USER_ID)
                .content("Hi")
                .createdAt(LocalDateTime.of(2026, 8, 12, 11, 0))
                .build();
        when(participantHelper.isParticipant(otherRequest, PATIENT_USER_ID)).thenReturn(true);
        when(chatMessageRepository.findByServiceRequest_IdOrderByCreatedAtAsc(otherRequest))
                .thenReturn(List.of(msg));

        List<ChatMessageResponse> responses = service.getMessages(otherRequest, PATIENT_USER_ID, null);

        assertEquals(1, responses.size());
        assertEquals(otherRequest, responses.get(0).serviceRequestId());
        assertEquals("Hi", responses.get(0).content());
    }
}