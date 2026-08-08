package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.ChatMessageResponse;
import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.entity.ChatMessage;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.NotificationType;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ForbiddenException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.repository.ChatMessageRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.ChatMessageService;
import iti.jets.java.homenursing.service.NotificationService;
import iti.jets.java.homenursing.util.AfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final String CHAT_TOPIC = "/topic/chat/";

    private final ChatMessageRepository chatMessageRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final ReservationParticipantService participantService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AfterCommitExecutor afterCommitExecutor;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(UUID reservationId, UUID senderUserId, String content) {
        if (!participantService.isParticipant(reservationId, senderUserId)) {
            throw new ForbiddenException("Not a participant of this reservation");
        }
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Message content is required");
        }

        ServiceRequest serviceRequest = serviceRequestRepository.findWithDetailsById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        ChatMessage message = ChatMessage.builder()
                .serviceRequest(serviceRequest)
                .senderUserId(senderUserId)
                .content(content)
                .build();
        chatMessageRepository.save(message);

        User sender = userRepository.findById(senderUserId).orElse(null);

        ChatMessageResponse response = toResponse(message, sender);

        afterCommitExecutor.execute(() ->
                messagingTemplate.convertAndSend(CHAT_TOPIC + reservationId, response));

        notifyOtherParticipants(serviceRequest, senderUserId, reservationId);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(UUID reservationId, UUID userId, LocalDateTime after) {
        if (!participantService.isParticipant(reservationId, userId)) {
            throw new ForbiddenException("Not a participant of this reservation");
        }

        List<ChatMessage> messages;
        if (after != null) {
            messages = chatMessageRepository
                    .findByServiceRequest_IdAndCreatedAtAfterOrderByCreatedAtAsc(reservationId, after);
        } else {
            messages = chatMessageRepository
                    .findByServiceRequest_IdOrderByCreatedAtAsc(reservationId);
        }

        Map<UUID, User> senders = resolveSenders(messages);

        return messages.stream()
                .map(message -> toResponse(message, senders.get(message.getSenderUserId())))
                .toList();
    }

    private void notifyOtherParticipants(ServiceRequest serviceRequest, UUID senderUserId, UUID reservationId) {
        UUID patientUserId = serviceRequest.getProfile().getUser().getId();
        notifyIfNotSender(patientUserId, senderUserId, reservationId);

        if (serviceRequest.getNurse() != null) {
            UUID nurseUserId = serviceRequest.getNurse().getUser().getId();
            notifyIfNotSender(nurseUserId, senderUserId, reservationId);
        }
    }

    private void notifyIfNotSender(UUID recipientUserId, UUID senderUserId, UUID reservationId) {
        if (recipientUserId.equals(senderUserId)) {
            return;
        }
        notificationService.create(new NotificationRequest(
                recipientUserId,
                "New Message",
                "You have a new message for this reservation.",
                NotificationType.MESSAGE,
                "SERVICE_REQUEST",
                reservationId));
    }

    private Map<UUID, User> resolveSenders(List<ChatMessage> messages) {
        Set<UUID> senderIds = messages.stream()
                .map(ChatMessage::getSenderUserId)
                .collect(Collectors.toSet());
        if (senderIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private ChatMessageResponse toResponse(ChatMessage message, User sender) {
        String senderName = sender != null ? sender.getFirstName() + " " + sender.getLastName() : null;
        String senderPhone = sender != null ? sender.getPhoneNumber() : null;
        return new ChatMessageResponse(
                message.getId(),
                message.getServiceRequest().getId(),
                message.getSenderUserId(),
                senderName,
                senderPhone,
                message.getContent(),
                message.getCreatedAt());
    }
}
