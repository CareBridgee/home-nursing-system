package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.service.ChatMessageService;
import iti.jets.java.homenursing.service.NurseOfferService;
import iti.jets.java.homenursing.service.ServiceRequestService;
import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketControllerTest {

    private final NurseRepository nurseRepository = mock(NurseRepository.class);
    private final WebSocketController controller = new WebSocketController(
            mock(SimpMessagingTemplate.class),
            mock(WebSocketPresenceService.class),
            mock(NurseOfferService.class),
            mock(ServiceRequestService.class),
            mock(ChatMessageService.class),
            nurseRepository);

    private static final UUID USER_ID = UUID.randomUUID();

    private Principal authenticated(String role) {
        return new UsernamePasswordAuthenticationToken(
                USER_ID.toString(), null,
                List.of(new SimpleGrantedAuthority(role)));
    }

    private Principal plainPrincipal() {
        return () -> USER_ID.toString();
    }

    @Test
    void heartbeat_withPlainPrincipal_isRejected() {
        assertThrows(SecurityException.class, () -> controller.heartbeat(plainPrincipal()));
    }

    @Test
    void heartbeat_byPatient_isRejected() {
        assertThrows(SecurityException.class,
                () -> controller.heartbeat(authenticated("ROLE_PATIENT")));
    }

    @Test
    void heartbeat_byNurseWithoutRecord_isRejected() {
        when(nurseRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());
        assertThrows(SecurityException.class,
                () -> controller.heartbeat(authenticated("ROLE_NURSE")));
    }

    @Test
    void heartbeat_byUnapprovedNurse_isRejected() {
        when(nurseRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(nurse(VerificationStatus.UNDER_REVIEW)));
        assertThrows(SecurityException.class,
                () -> controller.heartbeat(authenticated("ROLE_NURSE")));
    }

    @Test
    void heartbeat_byApprovedNurse_isAllowed() {
        when(nurseRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(nurse(VerificationStatus.APPROVED)));
        assertDoesNotThrow(() -> controller.heartbeat(authenticated("ROLE_NURSE")));
    }

    private Nurse nurse(VerificationStatus status) {
        User user = new User();
        user.setId(USER_ID);
        Nurse nurse = new Nurse();
        nurse.setUser(user);
        nurse.setVerificationStatus(status);
        return nurse;
    }
}