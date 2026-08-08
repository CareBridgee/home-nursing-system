package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferUpdateRequest;
import iti.jets.java.homenursing.dto.reservation.ReservationEvent;
import iti.jets.java.homenursing.dto.ws.AvailabilityPayload;
import iti.jets.java.homenursing.dto.ws.ChatSendPayload;
import iti.jets.java.homenursing.dto.ws.LocationPayload;
import iti.jets.java.homenursing.dto.ws.OfferActionPayload;
import iti.jets.java.homenursing.dto.ws.OfferUpdatePayload;
import iti.jets.java.homenursing.dto.ws.ServiceRequestIdPayload;
import iti.jets.java.homenursing.service.ChatMessageService;
import iti.jets.java.homenursing.service.NurseOfferService;
import iti.jets.java.homenursing.service.ServiceRequestService;
import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketPresenceService presenceService;
    private final NurseOfferService nurseOfferService;
    private final ServiceRequestService serviceRequestService;
    private final ChatMessageService chatMessageService;

    public WebSocketController(SimpMessagingTemplate messagingTemplate,
                                WebSocketPresenceService presenceService,
                                NurseOfferService nurseOfferService,
                                ServiceRequestService serviceRequestService,
                                ChatMessageService chatMessageService) {
        this.messagingTemplate = messagingTemplate;
        this.presenceService = presenceService;
        this.nurseOfferService = nurseOfferService;
        this.serviceRequestService = serviceRequestService;
        this.chatMessageService = chatMessageService;
    }

    @MessageMapping("/heartbeat")
    public void heartbeat(Principal principal) {
        requireNurse(principal);
        presenceService.heartbeat(principal.getName());
    }

    @MessageMapping("/reservation/availability")
    public void toggleAvailability(@Valid @Payload AvailabilityPayload payload,
                                    Principal principal) {
        requireNurse(principal);
        String userId = principal.getName();
        if (Boolean.TRUE.equals(payload.available())) {
            presenceService.markAvailable(
                    userId,
                    payload.lat().doubleValue(),
                    payload.lng().doubleValue());
        } else {
            presenceService.markUnavailable(userId);
        }
    }

    @MessageMapping("/reservation/location")
    public void updateLocation(@Valid @Payload LocationPayload payload,
                                Principal principal) {
        requireNurse(principal);
        String userId = principal.getName();
        presenceService.markAvailable(
                userId,
                payload.lat().doubleValue(),
                payload.lng().doubleValue());
    }

    @MessageMapping("/reservation/offer/create")
    public void createOffer(@Valid @Payload NurseOfferRequest request,
                             Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        nurseOfferService.create(userId, request);
    }

    @MessageMapping("/reservation/offer/update")
    public void updateOffer(@Valid @Payload OfferUpdatePayload payload,
                             Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        NurseOfferUpdateRequest updateReq = toUpdateRequest(payload);
        nurseOfferService.update(payload.offerId(), userId, updateReq);
    }

    @MessageMapping("/reservation/offer/counter")
    public void counterOffer(@Valid @Payload OfferUpdatePayload update,
                              Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        NurseOfferUpdateRequest request = toUpdateRequest(update);
        nurseOfferService.counterOffer(update.offerId(), userId, request);
    }

    @MessageMapping("/reservation/offer/accept")
    public void acceptOffer(@Valid @Payload OfferActionPayload payload,
                             Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        nurseOfferService.accept(payload.offerId(), userId);
    }

    @MessageMapping("/reservation/offer/withdraw")
    public void withdrawOffer(@Valid @Payload OfferActionPayload payload,
                               Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        nurseOfferService.withdraw(payload.offerId(), userId);
    }

    @MessageMapping("/reservation/offer/reject")
    public void rejectOffer(@Valid @Payload OfferActionPayload payload,
                             Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        nurseOfferService.reject(payload.offerId(), userId);
    }

    @MessageMapping("/reservation/cancel")
    public void cancelRequest(@Valid @Payload ServiceRequestIdPayload payload,
                               Principal principal) {
        serviceRequestService.cancelRequest(payload.serviceRequestId(), UUID.fromString(principal.getName()));
    }

    @MessageMapping("/reservation/offers/list")
    public void listOffers(@Valid @Payload ServiceRequestIdPayload payload,
                            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        List<NurseOfferResponse> offers = nurseOfferService.listByServiceRequest(payload.serviceRequestId(), userId);

        pushEvent(payload.serviceRequestId(), "OFFERS_LIST", offers);
    }

    @MessageMapping("/chat/{reservationId}/send")
    public void sendChatMessage(@DestinationVariable UUID reservationId,
                                 @Valid @Payload ChatSendPayload payload,
                                 Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        chatMessageService.sendMessage(reservationId, senderId, payload.content());
    }

    private void requireNurse(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getAuthorities().stream()
                        .noneMatch(a -> a.getAuthority().equals("ROLE_NURSE"))) {
            throw new SecurityException("Only nurses can use presence endpoints");
        }
    }

    private void pushEvent(UUID reservationId, String type, Object data) {
        messagingTemplate.convertAndSend(
                "/topic/reservation/" + reservationId,
                new ReservationEvent(type, reservationId, data));
    }

    private NurseOfferUpdateRequest toUpdateRequest(OfferUpdatePayload payload) {
        return new NurseOfferUpdateRequest(
                payload.proposedPrice(),
                payload.proposedDate(),
                payload.proposedTime(),
                payload.message());
    }
}