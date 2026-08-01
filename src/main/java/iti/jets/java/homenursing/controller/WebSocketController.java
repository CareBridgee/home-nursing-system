package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferUpdateRequest;
import iti.jets.java.homenursing.dto.reservation.ReservationEvent;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.enums.NotificationType;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.service.ChatMessageService;
import iti.jets.java.homenursing.service.NurseOfferService;
import iti.jets.java.homenursing.service.NotificationService;
import iti.jets.java.homenursing.service.ServiceRequestService;
import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class WebSocketController {

    private final ServiceRequestRepository serviceRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketPresenceService presenceService;
    private final NurseOfferService nurseOfferService;
    private final ServiceRequestService serviceRequestService;
    private final NotificationService notificationService;
    private final ChatMessageService chatMessageService;

    public WebSocketController(ServiceRequestRepository serviceRequestRepository,
                                SimpMessagingTemplate messagingTemplate,
                                WebSocketPresenceService presenceService,
                                NurseOfferService nurseOfferService,
                                ServiceRequestService serviceRequestService,
                                NotificationService notificationService,
                                ChatMessageService chatMessageService) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.messagingTemplate = messagingTemplate;
        this.presenceService = presenceService;
        this.nurseOfferService = nurseOfferService;
        this.serviceRequestService = serviceRequestService;
        this.notificationService = notificationService;
        this.chatMessageService = chatMessageService;
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

    @MessageMapping("/reservation/offer/create")
    public void createOffer(@Payload NurseOfferRequest request,
                             Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        NurseOfferResponse response = nurseOfferService.create(userId, request);

        UUID reservationId = request.serviceRequestId();
        pushEvent(reservationId, "OFFER_CREATED", response);

        notifyUserOfServiceRequest(reservationId, "New Offer Received",
                "A nurse has submitted an offer for your service request.",
                NotificationType.BOOKING, reservationId);
    }

    @MessageMapping("/reservation/offer/update")
    public void updateOffer(@Payload Map<String, Object> payload,
                             Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        UUID offerId = UUID.fromString((String) payload.get("offerId"));
        NurseOfferUpdateRequest updateReq = buildUpdateRequest(payload);
        NurseOfferResponse response = nurseOfferService.update(offerId, userId, updateReq);

        pushEvent(response.serviceRequestId(), "OFFER_UPDATED", response);

        notifyUserOfServiceRequest(response.serviceRequestId(), "Offer Terms Updated",
                "The nurse has updated their offer terms.",
                NotificationType.BOOKING, response.serviceRequestId());
    }

    @MessageMapping("/reservation/offer/counter")
    public void counterOffer(@Payload Map<String, Object> payload,
                              Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        UUID offerId = UUID.fromString((String) payload.get("offerId"));
        NurseOfferUpdateRequest request = buildUpdateRequest(payload);
        NurseOfferResponse response = nurseOfferService.counterOffer(offerId, userId, request);

        pushEvent(response.serviceRequestId(), "OFFER_COUNTERED", response);

        notifyUserOfServiceRequest(response.serviceRequestId(), "Counter-Offer Received",
                "The patient has proposed new terms.",
                NotificationType.BOOKING, response.serviceRequestId());
    }

    @MessageMapping("/reservation/offer/accept")
    public void acceptOffer(@Payload Map<String, Object> payload,
                             Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        UUID offerId = UUID.fromString((String) payload.get("offerId"));
        NurseOfferResponse response = nurseOfferService.accept(offerId, userId);

        pushEvent(response.serviceRequestId(), "OFFER_ACCEPTED", response);

        notifyUserOfServiceRequest(response.serviceRequestId(), "Offer Accepted",
                "An offer has been accepted. Reservation confirmed!",
                NotificationType.BOOKING, response.serviceRequestId());
    }

    @MessageMapping("/reservation/offer/withdraw")
    public void withdrawOffer(@Payload Map<String, Object> payload,
                               Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        UUID offerId = UUID.fromString((String) payload.get("offerId"));
        nurseOfferService.withdraw(offerId, userId);

        ServiceRequest request = serviceRequestRepository.findWithDetailsById(
                nurseOfferService.get(offerId, userId).serviceRequestId()).orElse(null);
        if (request != null) {
            pushEvent(request.getId(), "OFFER_WITHDRAWN", Map.of("offerId", offerId));

            notifyUserOfServiceRequest(request.getId(), "Offer Withdrawn",
                    "A nurse has withdrawn their offer.",
                    NotificationType.BOOKING, request.getId());
        }
    }

    @MessageMapping("/reservation/offer/reject")
    public void rejectOffer(@Payload Map<String, Object> payload,
                             Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        UUID offerId = UUID.fromString((String) payload.get("offerId"));
        nurseOfferService.reject(offerId, userId);

        ServiceRequest request = serviceRequestRepository.findWithDetailsById(
                nurseOfferService.get(offerId, userId).serviceRequestId()).orElse(null);
        if (request != null) {
            pushEvent(request.getId(), "OFFER_REJECTED", Map.of("offerId", offerId));

            notifyUserOfServiceRequest(request.getId(), "Offer Rejected",
                    "The patient has rejected your offer.",
                    NotificationType.BOOKING, request.getId());
        }
    }

    @MessageMapping("/reservation/cancel")
    public void cancelRequest(@Payload Map<String, Object> payload,
                               Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        UUID serviceRequestId = UUID.fromString((String) payload.get("serviceRequestId"));
        serviceRequestService.cancelRequest(serviceRequestId, userId);

        pushEvent(serviceRequestId, "REQUEST_CANCELLED", Map.of());

        notifyUserOfServiceRequest(serviceRequestId, "Request Cancelled",
                "The service request has been cancelled.",
                NotificationType.BOOKING, serviceRequestId);
    }

    @MessageMapping("/reservation/offers/list")
    public void listOffers(@Payload Map<String, Object> payload,
                            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        UUID serviceRequestId = UUID.fromString((String) payload.get("serviceRequestId"));
        List<NurseOfferResponse> offers = nurseOfferService.listByServiceRequest(serviceRequestId, userId);

        pushEvent(serviceRequestId, "OFFERS_LIST", offers);
    }

    @MessageMapping("/chat/{reservationId}/send")
    public void sendChatMessage(@DestinationVariable UUID reservationId,
                                 @Payload Map<String, Object> payload,
                                 Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        String content = (String) payload.get("content");
        chatMessageService.sendMessage(reservationId, senderId, content);
    }

    private void pushEvent(UUID reservationId, String type, Object data) {
        messagingTemplate.convertAndSend(
                "/topic/reservation/" + reservationId,
                new ReservationEvent(type, reservationId, data));
    }

    private void notifyUserOfServiceRequest(UUID serviceRequestId, String title,
                                             String message, NotificationType type,
                                             UUID relatedEntityId) {
        ServiceRequest request = serviceRequestRepository.findWithDetailsById(serviceRequestId).orElse(null);
        if (request == null) return;

        UUID patientUserId = request.getProfile().getUser().getId();

        NotificationRequest notification = new NotificationRequest(
                patientUserId, title, message, type,
                "SERVICE_REQUEST", relatedEntityId);
        notificationService.create(notification);

        if (request.getNurse() != null) {
            UUID nurseUserId = request.getNurse().getUser().getId();
            if (!nurseUserId.equals(patientUserId)) {
                NotificationRequest nurseNotif = new NotificationRequest(
                        nurseUserId, title, message, type,
                        "SERVICE_REQUEST", relatedEntityId);
                notificationService.create(nurseNotif);
            }
        }
    }

    private NurseOfferUpdateRequest buildUpdateRequest(Map<String, Object> payload) {
        java.math.BigDecimal price = null;
        if (payload.get("proposedPrice") != null) {
            Object priceVal = payload.get("proposedPrice");
            if (priceVal instanceof Number num) {
                price = java.math.BigDecimal.valueOf(num.doubleValue());
            } else if (priceVal instanceof String s) {
                price = new java.math.BigDecimal(s);
            }
        }
        java.time.LocalDate date = null;
        if (payload.get("proposedDate") != null) {
            date = java.time.LocalDate.parse((String) payload.get("proposedDate"));
        }
        java.time.LocalTime time = null;
        if (payload.get("proposedTime") != null) {
            time = java.time.LocalTime.parse((String) payload.get("proposedTime"));
        }
        String message = (String) payload.get("message");
        return new NurseOfferUpdateRequest(price, date, time, message);
    }
}
