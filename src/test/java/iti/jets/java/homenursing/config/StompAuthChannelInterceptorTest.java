package iti.jets.java.homenursing.config;

import iti.jets.java.homenursing.service.TokenService;
import iti.jets.java.homenursing.util.ReservationParticipantHelper;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StompAuthChannelInterceptorTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private final TokenService tokenService = mock(TokenService.class);
    private final ReservationParticipantHelper participantHelper = mock(ReservationParticipantHelper.class);
    private final StompAuthChannelInterceptor interceptor =
            new StompAuthChannelInterceptor(tokenService, participantHelper);

    private Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError(cause);
        }
    }

    private StompHeaderAccessor accessor() {
        return mock(StompHeaderAccessor.class);
    }

    private void authenticated(StompHeaderAccessor accessor) {
        when(accessor.getUser()).thenReturn(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null));
    }

    @Test
    void preSend_passesThroughWhenAccessorIsMissing() {
        Message<?> message = mock(Message.class);
        assertSame(message, interceptor.preSend(message, null));
    }

    @Test
    void connect_withoutAuthorizationHeader_isRejected() {
        StompHeaderAccessor accessor = accessor();
        when(accessor.getNativeHeader("Authorization")).thenReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> invoke(interceptor, "authenticateConnect",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void connect_withEmptyAuthorizationHeaderList_isRejected() {
        StompHeaderAccessor accessor = accessor();
        when(accessor.getNativeHeader("Authorization")).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class,
                () -> invoke(interceptor, "authenticateConnect",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void connect_withNoBearerAnywhere_isRejected() {
        StompHeaderAccessor accessor = accessor();
        when(accessor.getNativeHeader("Authorization")).thenReturn(Arrays.asList("Basic xyz", "X-Custom k"));
        assertThrows(IllegalArgumentException.class,
                () -> invoke(interceptor, "authenticateConnect",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void connect_withValidRefreshToken_isRejected() {
        StompHeaderAccessor accessor = accessor();
        when(accessor.getNativeHeader("Authorization")).thenReturn(List.of("Bearer refresh-tok"));
        when(tokenService.isTokenValid("refresh-tok")).thenReturn(true);
        when(tokenService.isAccessToken("refresh-tok")).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> invoke(interceptor, "authenticateConnect",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void connect_scansHeaders_picksFirstBearerAndBreaks() throws Exception {
        StompHeaderAccessor accessor = accessor();
        when(accessor.getNativeHeader("Authorization"))
                .thenReturn(Arrays.asList("Basic xyz", null, "Bearer valid-token"));
        when(tokenService.isTokenValid("valid-token")).thenReturn(true);
        when(tokenService.isAccessToken("valid-token")).thenReturn(true);
        when(tokenService.getUserIdFromToken("valid-token")).thenReturn(USER_ID.toString());
        when(tokenService.getRoleFromToken("valid-token")).thenReturn("USER");

        invoke(interceptor, "authenticateConnect", new Class<?>[]{StompHeaderAccessor.class}, accessor);
        verify(accessor).setUser(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void connect_withInvalidToken_isRejected() {
        StompHeaderAccessor accessor = accessor();
        when(accessor.getNativeHeader("Authorization")).thenReturn(List.of("Bearer bad"));
        when(tokenService.isTokenValid("bad")).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> invoke(interceptor, "authenticateConnect",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void subscribe_withoutDestination_isAllowed() throws Exception {
        StompHeaderAccessor accessor = accessor();
        invoke(interceptor, "authorizeSubscribe",
                new Class<?>[]{StompHeaderAccessor.class}, accessor);
    }

    @Test
    void subscribe_withoutUser_isRejected() {
        StompHeaderAccessor accessor = accessor();
        when(accessor.getDestination()).thenReturn("/topic/reservation/" + UUID.randomUUID());
        assertThrows(IllegalArgumentException.class,
                () -> invoke(interceptor, "authorizeSubscribe",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void subscribe_ownUserQueue_isAllowed() throws Exception {
        StompHeaderAccessor accessor = accessor();
        authenticated(accessor);
        when(accessor.getDestination()).thenReturn("/user/" + USER_ID + "/queue/errors");
        invoke(interceptor, "authorizeSubscribe",
                new Class<?>[]{StompHeaderAccessor.class}, accessor);
    }

    @Test
    void subscribe_ownUserQueueWithoutSubpath_isAllowed() throws Exception {
        StompHeaderAccessor accessor = accessor();
        authenticated(accessor);
        when(accessor.getDestination()).thenReturn("/user/" + USER_ID);
        invoke(interceptor, "authorizeSubscribe",
                new Class<?>[]{StompHeaderAccessor.class}, accessor);
    }

    @Test
    void subscribe_otherUserQueue_isRejected() {
        StompHeaderAccessor accessor = accessor();
        authenticated(accessor);
        when(accessor.getDestination()).thenReturn("/user/" + UUID.randomUUID() + "/queue/errors");
        assertThrows(SecurityException.class,
                () -> invoke(interceptor, "authorizeSubscribe",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void subscribe_nonParticipantTopic_isRejected() {
        UUID reservationId = UUID.randomUUID();
        StompHeaderAccessor accessor = accessor();
        authenticated(accessor);
        when(accessor.getDestination()).thenReturn("/topic/reservation/" + reservationId);
        when(participantHelper.isParticipant(reservationId, USER_ID)).thenReturn(false);
        assertThrows(SecurityException.class,
                () -> invoke(interceptor, "authorizeSubscribe",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void subscribe_participantTopic_isAllowed() throws Exception {
        UUID reservationId = UUID.randomUUID();
        StompHeaderAccessor accessor = accessor();
        authenticated(accessor);
        when(accessor.getDestination()).thenReturn("/topic/chat/" + reservationId);
        when(participantHelper.isParticipant(reservationId, USER_ID)).thenReturn(true);
        invoke(interceptor, "authorizeSubscribe",
                new Class<?>[]{StompHeaderAccessor.class}, accessor);
    }

    @Test
    void subscribe_unrelatedDestination_isAllowed() throws Exception {
        StompHeaderAccessor accessor = accessor();
        authenticated(accessor);
        when(accessor.getDestination()).thenReturn("/topic/other");
        invoke(interceptor, "authorizeSubscribe",
                new Class<?>[]{StompHeaderAccessor.class}, accessor);
    }

    @Test
    void send_withoutDestination_isRejected() {
        StompHeaderAccessor accessor = accessor();
        authenticated(accessor);
        assertThrows(SecurityException.class,
                () -> invoke(interceptor, "authorizeSend",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void send_toTopicDestination_isRejected() {
        StompHeaderAccessor accessor = accessor();
        authenticated(accessor);
        when(accessor.getDestination()).thenReturn("/topic/foo");
        assertThrows(SecurityException.class,
                () -> invoke(interceptor, "authorizeSend",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void presenceSend_byPlainPrincipalUser_isRejected() {
        StompHeaderAccessor accessor = accessor();
        when(accessor.getUser()).thenReturn(() -> USER_ID.toString());
        when(accessor.getDestination()).thenReturn("/app/heartbeat");
        assertThrows(SecurityException.class,
                () -> invoke(interceptor, "authorizeSend",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void presenceSend_withoutAuthenticatedUser_isRejected() {
        StompHeaderAccessor accessor = accessor();
        when(accessor.getDestination()).thenReturn("/app/heartbeat");
        assertThrows(SecurityException.class,
                () -> invoke(interceptor, "authorizeSend",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void presenceSend_byOtherRole_isRejected() {
        StompHeaderAccessor accessor = accessor();
        when(accessor.getUser()).thenReturn(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PATIENT"))));
        when(accessor.getDestination()).thenReturn("/app/heartbeat");
        assertThrows(SecurityException.class,
                () -> invoke(interceptor, "authorizeSend",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void presenceSend_byTokenWithoutAuthorities_isRejected() {
        StompHeaderAccessor accessor = accessor();
        when(accessor.getUser()).thenReturn(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null, null));
        when(accessor.getDestination()).thenReturn("/app/reservation/location");
        assertThrows(SecurityException.class,
                () -> invoke(interceptor, "authorizeSend",
                        new Class<?>[]{StompHeaderAccessor.class}, accessor));
    }

    @Test
    void presenceSend_byNurseRole_isAllowed() throws Exception {
        StompHeaderAccessor accessor = accessor();
        when(accessor.getUser()).thenReturn(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_NURSE"))));
        when(accessor.getDestination()).thenReturn("/app/heartbeat");
        invoke(interceptor, "authorizeSend",
                new Class<?>[]{StompHeaderAccessor.class}, accessor);
    }

    @Test
    void userIdOf_returnsNullForUnauthenticatedAccessor() throws Exception {
        StompHeaderAccessor accessor = accessor();
        Method method = StompAuthChannelInterceptor.class.getDeclaredMethod("userIdOf", StompHeaderAccessor.class);
        method.setAccessible(true);
        assertSame(null, method.invoke(null, accessor));
    }
}