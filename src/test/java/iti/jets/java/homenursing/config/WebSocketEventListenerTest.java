package iti.jets.java.homenursing.config;

import iti.jets.java.homenursing.service.ServiceRequestService;
import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private WebSocketPresenceService presenceService;

    @Mock
    private ServiceRequestService serviceRequestService;

    @InjectMocks
    private WebSocketEventListener listener;

    @Test
    void sessionConnected_nurseGoesOnline() {
        SessionConnectedEvent event = new SessionConnectedEvent(this,
                message(StompCommand.CONNECT, auth("11111111-1111-1111-1111-111111111111", "ROLE_NURSE")));

        listener.handleSessionConnected(event);

        verify(presenceService).markOnline("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void sessionConnected_anonymousIgnored() {
        SessionConnectedEvent event = new SessionConnectedEvent(this, message(StompCommand.CONNECT, null));

        listener.handleSessionConnected(event);

        verify(presenceService, never()).markOnline(anyString());
    }

    @Test
    void sessionDisconnected_nurseGoesOfflineAndUnavailable() {
        SessionDisconnectEvent event = new SessionDisconnectEvent(this,
                message(StompCommand.DISCONNECT, auth("11111111-1111-1111-1111-111111111111", "ROLE_NURSE")),
                "s1", CloseStatus.NORMAL);

        listener.handleSessionDisconnect(event);

        verify(presenceService).markOffline("11111111-1111-1111-1111-111111111111");
        verify(presenceService).markUnavailable("11111111-1111-1111-1111-111111111111");
        verify(serviceRequestService, never()).cancelOpenRequestsForUser(any());
    }

    @Test
    void sessionDisconnected_patientCancelsOpenRequests() {
        SessionDisconnectEvent event = new SessionDisconnectEvent(this,
                message(StompCommand.DISCONNECT, auth("22222222-2222-2222-2222-222222222222", "ROLE_USER")),
                "s1", CloseStatus.NORMAL);

        listener.handleSessionDisconnect(event);

        verify(serviceRequestService).cancelOpenRequestsForUser(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    }

    @Test
    void sessionDisconnected_anonymousLoggedAndIgnored() {
        SessionDisconnectEvent event = new SessionDisconnectEvent(this,
                message(StompCommand.DISCONNECT, null),
                "s1", CloseStatus.NORMAL);

        listener.handleSessionDisconnect(event);

        verify(presenceService, never()).markOffline(anyString());
        verify(serviceRequestService, never()).cancelOpenRequestsForUser(any());
    }

    @Test
    void sessionUnsubscribed_patientCancelsOpenRequests() {
        StompHeaderAccessor accessor = accessor(StompCommand.UNSUBSCRIBE, auth("22222222-2222-2222-2222-222222222222", "ROLE_USER"));
        accessor.setDestination("/topic/reservation/any");
        SessionUnsubscribeEvent event =
                new SessionUnsubscribeEvent(this, MessageBuilder.createMessage(new byte[0],
                        accessor.getMessageHeaders()));

        listener.handleSessionUnsubscribe(event);

        verify(serviceRequestService).cancelOpenRequestsForUser(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    }

    @Test
    void sessionUnsubscribed_nurseDoesNotCancelRequests() {
        StompHeaderAccessor accessor = accessor(StompCommand.UNSUBSCRIBE, auth("11111111-1111-1111-1111-111111111111", "ROLE_NURSE"));
        accessor.setDestination("/topic/reservation/any");
        SessionUnsubscribeEvent event =
                new SessionUnsubscribeEvent(this, MessageBuilder.createMessage(new byte[0],
                        accessor.getMessageHeaders()));

        listener.handleSessionUnsubscribe(event);

        verify(serviceRequestService, never()).cancelOpenRequestsForUser(any());
    }

    @Test
    void sessionUnsubscribed_anonymousIgnored() {
        StompHeaderAccessor accessor = accessor(StompCommand.UNSUBSCRIBE, null);
        accessor.setDestination("/topic/reservation/any");
        SessionUnsubscribeEvent event =
                new SessionUnsubscribeEvent(this, MessageBuilder.createMessage(new byte[0],
                        accessor.getMessageHeaders()));

        listener.handleSessionUnsubscribe(event);

        verify(serviceRequestService, never()).cancelOpenRequestsForUser(any());
    }

    @Test
    void disconnect_swallowsCancelFailures() {
        ServiceRequestService failing = mock(ServiceRequestService.class);
        doThrow(new IllegalStateException("boom")).when(failing).cancelOpenRequestsForUser(any());
        WebSocketEventListener guarded = new WebSocketEventListener(presenceService, failing);

        SessionDisconnectEvent event = new SessionDisconnectEvent(this,
                message(StompCommand.DISCONNECT, auth("22222222-2222-2222-2222-222222222222", "ROLE_USER")),
                "s1", CloseStatus.NORMAL);

        guarded.handleSessionDisconnect(event);
    }

    private static Message<byte[]> message(StompCommand command, Principal user) {
        return MessageBuilder.createMessage(new byte[0], accessor(command, user).getMessageHeaders());
    }

    private static StompHeaderAccessor accessor(StompCommand command, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("s1");
        if (user != null) {
            accessor.setUser(user);
        }
        return accessor;
    }

    private static Principal auth(String name, String role) {
        return new UsernamePasswordAuthenticationToken(
                name, null, List.of(new SimpleGrantedAuthority(role)));
    }
}