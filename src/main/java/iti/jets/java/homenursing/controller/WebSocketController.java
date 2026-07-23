package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.ChatMessageResponse;
import iti.jets.java.homenursing.entity.ChatMessage;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.repository.ChatMessageRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.service.impl.ReservationParticipantService;
import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Controller
public class WebSocketController {

    private final ChatMessageRepository chatMessageRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ReservationParticipantService participantService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketPresenceService presenceService;

    public WebSocketController(ChatMessageRepository chatMessageRepository,
                                ServiceRequestRepository serviceRequestRepository,
                                ReservationParticipantService participantService,
                                SimpMessagingTemplate messagingTemplate,
                                WebSocketPresenceService presenceService) {
        this.chatMessageRepository = chatMessageRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.participantService = participantService;
        this.messagingTemplate = messagingTemplate;
        this.presenceService = presenceService;
    }

    @MessageMapping("/heartbeat")
    public void heartbeat(Principal principal) {
        String userId = principal.getName();
        if (presenceService.isOnline(userId)) {
            presenceService.refreshAvailabilityTimestamp(userId);
        }
    }

    @MessageMapping("/reservation/availability")
    public void toggleAvailability(@Payload Map<String, Object> payload,
                                    Principal principal) {
        String userId = principal.getName();
        boolean available = Boolean.TRUE.equals(payload.get("available"));
        if (available) {
            double lat = ((Number) payload.get("lat")).doubleValue();
            double lng = ((Number) payload.get("lng")).doubleValue();
            presenceService.markAvailable(userId, lat, lng);
        } else {
            presenceService.markUnavailable(userId);
        }
    }

    @MessageMapping("/reservation/location")
    public void updateLocation(@Payload Map<String, Object> payload,
                                Principal principal) {
        String userId = principal.getName();
        double lat = ((Number) payload.get("lat")).doubleValue();
        double lng = ((Number) payload.get("lng")).doubleValue();
        presenceService.markAvailable(userId, lat, lng);
    }

    @MessageMapping("/reservation/request")
    public void requestService(@Payload Map<String, Object> payload,
                                Principal principal) {
        String userId = principal.getName();
        // TODO: implement full reservation flow
        // 1. Create ServiceRequest with PENDING status
        // 2. Query presenceService.findAvailableNearby(lat, lng, radius)
        // 3. For each nearby nurse, create NurseOffer
        // 4. Broadcast /topic/reservation/{id} to eligible nurses
    }

    @MessageMapping("/reservation/offer")
    public void nurseOffer(@Payload Map<String, Object> payload,
                            Principal principal) {
        // TODO: handle nurse counter-offer, accept/reject
    }

    @MessageMapping("/chat/{reservationId}/send")
    public void sendChatMessage(@DestinationVariable UUID reservationId,
                                 @Payload Map<String, Object> payload,
                                 Principal principal) {
        String userId = principal.getName();
        UUID senderId = UUID.fromString(userId);

        if (!participantService.isParticipant(reservationId, senderId)) {
            return;
        }

        String content = (String) payload.get("content");
        if (content == null || content.isBlank()) return;

        ServiceRequest serviceRequest = serviceRequestRepository.findById(reservationId).orElse(null);
        if (serviceRequest == null) return;

        ChatMessage message = ChatMessage.builder()
                .serviceRequest(serviceRequest)
                .senderUserId(senderId)
                .content(content)
                .build();
        chatMessageRepository.save(message);

        ChatMessageResponse response = new ChatMessageResponse(
                message.getId(), reservationId, senderId, content, message.getCreatedAt());

        messagingTemplate.convertAndSend("/topic/chat/" + reservationId, response);
    }
}
