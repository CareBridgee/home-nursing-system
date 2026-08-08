package iti.jets.java.homenursing.config;

import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketEventListener {

    private final WebSocketPresenceService presenceService;

    public WebSocketEventListener(WebSocketPresenceService presenceService) {
        this.presenceService = presenceService;
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
        presenceService.markOffline(userId);
        presenceService.markUnavailable(userId);
        log.debug("WS presence disconnected: userId={}, session={}", userId, accessor.getSessionId());
    }
}