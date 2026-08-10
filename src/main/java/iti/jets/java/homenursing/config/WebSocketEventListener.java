package iti.jets.java.homenursing.config;

import iti.jets.java.homenursing.service.ServiceRequestService;
import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.UUID;

@Slf4j
@Component
public class WebSocketEventListener {

    private final WebSocketPresenceService presenceService;
    private final ServiceRequestService serviceRequestService;

    public WebSocketEventListener(WebSocketPresenceService presenceService,
                                  ServiceRequestService serviceRequestService) {
        this.presenceService = presenceService;
        this.serviceRequestService = serviceRequestService;
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
        log.debug("WS session connected: session={}, user={}, nurse={}",
                accessor.getSessionId(), userId, isNurse);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication user = (Authentication) accessor.getUser();
        if (user == null) {
            log.warn("WS session disconnected without identity: session={}", accessor.getSessionId());
            return;
        }

        String userId = user.getName();
        boolean isNurse = user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_NURSE"));
        if (isNurse) {
            presenceService.markOffline(userId);
            presenceService.markUnavailable(userId);
        } else {
            cancelPatientOpenRequests(userId);
        }
        log.debug("WS presence disconnected: userId={}, session={}, nurse={}", userId, accessor.getSessionId(), isNurse);
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication user = (Authentication) accessor.getUser();
        if (user == null) return;

        String userId = user.getName();
        boolean isNurse = user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_NURSE"));
        if (!isNurse) {
            cancelPatientOpenRequests(userId);
        }
        log.debug("WS unsubscribe: userId={}, session={}, destination={}", userId,
                accessor.getSessionId(), accessor.getDestination());
    }

    private void cancelPatientOpenRequests(String userId) {
        try {
            serviceRequestService.cancelOpenRequestsForUser(UUID.fromString(userId));
        } catch (Exception e) {
            log.warn("Failed to cancel open requests for patient userId={}: {}", userId, e.getMessage());
        }
    }
}