package iti.jets.java.homenursing.config;

import iti.jets.java.homenursing.service.TokenService;
import iti.jets.java.homenursing.service.impl.ReservationParticipantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern RESERVATION_TOPIC = Pattern.compile("^/topic/reservation/([0-9a-f-]+)$");
    private static final Pattern CHAT_TOPIC = Pattern.compile("^/topic/chat/([0-9a-f-]+)$");

    private final TokenService tokenService;
    private final ReservationParticipantService participantService;

    public StompAuthChannelInterceptor(TokenService tokenService,
                                       ReservationParticipantService participantService) {
        this.tokenService = tokenService;
        this.participantService = participantService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            authorizeSend(accessor);
        }

        return message;
    }

    private void authorizeSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/app/")) {
            log.warn("WS SEND rejected: session={}, user={}, destination={}, reason=clients may only send to /app/ destinations",
                    accessor.getSessionId(), userIdOf(accessor), destination);
            throw new SecurityException("Clients may only send to /app/ destinations");
        }

        boolean presenceEndpoint = destination.equals("/app/heartbeat")
                || destination.equals("/app/reservation/availability")
                || destination.equals("/app/reservation/location");
        if (presenceEndpoint && !hasRole(accessor, "ROLE_NURSE")) {
            log.warn("WS SEND rejected: session={}, user={}, destination={}, reason=non-nurse used presence endpoint",
                    accessor.getSessionId(), userIdOf(accessor), destination);
            throw new SecurityException("Only nurses can use presence endpoints");
        }
    }

    private static String userIdOf(StompHeaderAccessor accessor) {
        return accessor.getUser() != null ? accessor.getUser().getName() : null;
    }

    private static boolean hasRole(StompHeaderAccessor accessor, String role) {
        var auth = accessor.getUser();
        if (!(auth instanceof UsernamePasswordAuthenticationToken token) || token.getAuthorities() == null) {
            return false;
        }
        return token.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            throw new IllegalArgumentException("Missing Authorization header in CONNECT frame");
        }

        String token = null;
        for (String header : authHeaders) {
            String trimmed = header != null ? header.trim() : "";
            if (trimmed.length() > 7 && trimmed.substring(0, 7).equalsIgnoreCase("Bearer ")) {
                token = trimmed.substring(7).trim();
                break;
            }
        }

        if (token == null || !tokenService.isTokenValid(token) || !tokenService.isAccessToken(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        String userId = tokenService.getUserIdFromToken(token);
        String role = tokenService.getRoleFromToken(token);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        accessor.setUser(auth);
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        if (accessor.getUser() == null) {
            throw new IllegalArgumentException("Authentication required to subscribe");
        }
        UUID userId = UUID.fromString(accessor.getUser().getName());

        if (destination.startsWith("/user/")) {
            String scopedUserId = destination.substring("/user/".length());
            int slash = scopedUserId.indexOf('/');
            if (slash > 0) {
                scopedUserId = scopedUserId.substring(0, slash);
            }
            if (isUuid(scopedUserId) && !scopedUserId.equals(userId.toString())) {
                log.warn("WS SUBSCRIBE rejected: session={}, user={}, destination={}, reason=subscribed to another user's /user/ destination",
                        accessor.getSessionId(), userId, destination);
                throw new SecurityException("You may only subscribe to your own /user/ destinations");
            }
            return;
        }

        UUID reservationId = null;
        Matcher reservationMatcher = RESERVATION_TOPIC.matcher(destination);
        if (reservationMatcher.matches()) {
            reservationId = UUID.fromString(reservationMatcher.group(1));
        } else {
            Matcher chatMatcher = CHAT_TOPIC.matcher(destination);
            if (chatMatcher.matches()) {
                reservationId = UUID.fromString(chatMatcher.group(1));
            }
        }
        if (reservationId == null) {
            return;
        }

        if (!participantService.isParticipant(reservationId, userId)) {
            log.warn("WS SUBSCRIBE rejected: session={}, user={}, destination={}, reason=not a participant",
                    accessor.getSessionId(), userId, destination);
            throw new SecurityException("Not a participant of this reservation");
        }
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}