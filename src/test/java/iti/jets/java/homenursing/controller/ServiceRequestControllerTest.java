package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.nurse.NearbyNurse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyNurseServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.service.ServiceRequestService;
import iti.jets.java.homenursing.util.PriceEstimator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServiceRequestControllerTest {

    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID NURSE_ID = UUID.randomUUID();

    private final ServiceRequestService serviceRequestService = mock(ServiceRequestService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final NurseRepository nurseRepository = mock(NurseRepository.class);
    private final ServiceRequestRepository serviceRequestRepository = mock(ServiceRequestRepository.class);
    private final PriceEstimator priceEstimator = mock(PriceEstimator.class);
    private final ServiceRequestController controller = new ServiceRequestController(
            serviceRequestService, messagingTemplate, nurseRepository, serviceRequestRepository, priceEstimator);

    private void invokePush(NearbyServiceRequestResponse response) throws Exception {
        Method method = ServiceRequestController.class.getDeclaredMethod("pushToNearbyNurses",
                NearbyServiceRequestResponse.class);
        method.setAccessible(true);
        method.invoke(controller, response);
    }

    private NearbyServiceRequestResponse response(List<NearbyNurse> nurses) {
        return new NearbyServiceRequestResponse(
                REQUEST_ID, UUID.randomUUID(), UUID.randomUUID(), ServiceRequestStatus.SEARCHING,
                new BigDecimal("30.0444"), new BigDecimal("31.2357"), nurses, LocalDateTime.now());
    }

    private NearbyNurse nearbyNurse() {
        return new NearbyNurse(NURSE_ID, new BigDecimal("30.04"), new BigDecimal("31.23"), 2.5);
    }

    @Test
    void pushToNearbyNurses_withNullNearbyNurses_skipsEntirely() throws Exception {
        invokePush(response(null));
        verifyNoInteractions(serviceRequestRepository);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void pushToNearbyNurses_withMissingRequestOrServiceType_skips() throws Exception {
        when(serviceRequestRepository.findWithDetailsById(REQUEST_ID)).thenReturn(Optional.empty());
        invokePush(response(List.of(nearbyNurse())));
        verifyNoInteractions(messagingTemplate);

        ServiceRequest noType = mock(ServiceRequest.class);
        when(noType.getServiceType()).thenReturn(null);
        when(serviceRequestRepository.findWithDetailsById(REQUEST_ID)).thenReturn(Optional.of(noType));
        invokePush(response(List.of(nearbyNurse())));
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void pushToNearbyNurses_withMissingNurseProfile_usesNullNames() throws Exception {
        ServiceRequest request = mock(ServiceRequest.class);
        ServiceType serviceType = mock(ServiceType.class);
        when(request.getServiceType()).thenReturn(serviceType);
        when(serviceType.getName()).thenReturn("Home Nursing");
        when(request.getProfile()).thenReturn(null);
        when(serviceRequestRepository.findWithDetailsById(REQUEST_ID)).thenReturn(Optional.of(request));
        Nurse nurse = profilelessNurse();
        when(nurseRepository.findWithUserById(NURSE_ID)).thenReturn(Optional.of(nurse));

        invokePush(response(List.of(nearbyNurse())));

        ArgumentCaptor<NearbyNurseServiceRequestResponse> captor =
                ArgumentCaptor.forClass(NearbyNurseServiceRequestResponse.class);
        verify(messagingTemplate).convertAndSendToUser(eq(NURSE_ID.toString()),
                eq("/queue/nearby-request"), captor.capture());
        assertThat(captor.getValue().patientFirstName(), is(nullValue()));
        assertThat(captor.getValue().patientLastName(), is(nullValue()));
    }

    @Test
    void pushToNearbyNurses_withMissingNurse_skipsThatNurse() throws Exception {
        ServiceRequest request = mock(ServiceRequest.class);
        ServiceType serviceType = mock(ServiceType.class);
        when(request.getServiceType()).thenReturn(serviceType);
        when(request.getProfile()).thenReturn(null);
        when(serviceRequestRepository.findWithDetailsById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.findWithUserById(NURSE_ID)).thenReturn(Optional.empty());

        invokePush(response(List.of(nearbyNurse())));
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void pushToNearbyNurses_happyPath_pushesPayloadWithPatientInfo() throws Exception {
        ServiceRequest request = mock(ServiceRequest.class);
        ServiceType serviceType = mock(ServiceType.class);
        iti.jets.java.homenursing.entity.Profile profile = mock(iti.jets.java.homenursing.entity.Profile.class);
        when(request.getServiceType()).thenReturn(serviceType);
        when(serviceType.getName()).thenReturn("Home Nursing");
        when(serviceType.getEstimatedDurationMinutes()).thenReturn(90);
        when(serviceType.getBasePrice()).thenReturn(new BigDecimal("500.00"));
        when(request.getProfile()).thenReturn(profile);
        when(profile.getFirstName()).thenReturn("Sara");
        when(profile.getLastName()).thenReturn("Ali");
        when(serviceRequestRepository.findWithDetailsById(REQUEST_ID)).thenReturn(Optional.of(request));
        Nurse nurse = mock(Nurse.class);
        User user = mock(User.class);
        when(nurse.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(NURSE_ID);
        when(nurseRepository.findWithUserById(NURSE_ID)).thenReturn(Optional.of(nurse));
        when(priceEstimator.estimate(new BigDecimal("500.00"), 2.5))
                .thenReturn(new BigDecimal("575.00"));

        invokePush(response(List.of(nearbyNurse())));

        ArgumentCaptor<NearbyNurseServiceRequestResponse> captor =
                ArgumentCaptor.forClass(NearbyNurseServiceRequestResponse.class);
        verify(messagingTemplate).convertAndSendToUser(eq(NURSE_ID.toString()),
                eq("/queue/nearby-request"), captor.capture());
        assertThat(captor.getValue().patientFirstName(), is("Sara"));
        assertThat(captor.getValue().patientLastName(), is("Ali"));
        assertThat(captor.getValue().serviceName(), is("Home Nursing"));
        assertThat(captor.getValue().estimatedPrice(), is(new BigDecimal("575.00")));
    }

    private Nurse profilelessNurse() {
        Nurse nurse = mock(Nurse.class);
        User user = mock(User.class);
        when(nurse.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(NURSE_ID);
        return nurse;
    }
}