package iti.jets.java.homenursing.config;

import iti.jets.java.homenursing.service.impl.ReservationParticipantService;
import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebSocketEventListener {

    private static final Pattern RESERVATION_TOPIC = Pattern.compile("^/topic/reservation/([0-9a-f-]+)$");
    private static final Pattern CHAT_TOPIC = Pattern.compile("^/topic/chat/([0-9a-f-]+)$");

    private final WebSocketPresenceService presenceService;
    private final ReservationParticipantService participantService;

    public WebSocketEventListener(WebSocketPresenceService presenceService,
                                  ReservationParticipantService participantService) {
        this.presenceService = presenceService;
        this.participantService = participantService;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication user = (Authentication) accessor.getUser();
        if (user == null) return;

        String userId = user.getName();
        boolean isNurse = user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_NURSE"));
        if (isNurse) {
            presenceService.markOnline(userId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication user = (Authentication) accessor.getUser();
        if (user == null) return;

        String userId = user.getName();
        presenceService.markOffline(userId);
        presenceService.markUnavailable(userId);
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        Message<?> message = event.getMessage();
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return;

        Authentication user = (Authentication) accessor.getUser();
        if (user == null) return;

        String destination = accessor.getDestination();
        if (destination == null) return;

        Matcher reservationMatcher = RESERVATION_TOPIC.matcher(destination);
        if (reservationMatcher.matches()) {
            UUID reservationId = UUID.fromString(reservationMatcher.group(1));
            UUID userId = UUID.fromString(user.getName());
            if (!participantService.isParticipant(reservationId, userId)) {
                throw new SecurityException("Not a participant of this reservation");
            }
            return;
        }

        Matcher chatMatcher = CHAT_TOPIC.matcher(destination);
        if (chatMatcher.matches()) {
            UUID reservationId = UUID.fromString(chatMatcher.group(1));
            UUID userId = UUID.fromString(user.getName());
            if (!participantService.isParticipant(reservationId, userId)) {
                throw new SecurityException("Not a participant of this reservation");
            }
        }
    }
}
