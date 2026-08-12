package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.nurse.NurseSummaryResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NearbyNurseOfferResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferUpdateRequest;
import iti.jets.java.homenursing.dto.reservation.ReservationEvent;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseOffer;
import iti.jets.java.homenursing.entity.NurseService;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.NurseOfferMapper;
import iti.jets.java.homenursing.repository.NurseOfferRepository;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.NurseServiceRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.service.impl.NurseOfferServiceImpl;
import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import iti.jets.java.homenursing.util.AfterCommitExecutor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NurseOfferServiceImplTest {

    @Mock
    private NurseOfferRepository nurseOfferRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private NurseRepository nurseRepository;
    @Mock
    private NurseServiceRepository nurseServiceRepository;
    @Mock
    private NurseOfferMapper nurseOfferMapper;
    @Mock
    private WebSocketPresenceService webSocketPresenceService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    @InjectMocks
    private NurseOfferServiceImpl offerService;

    private static final UUID PATIENT_USER_ID = UUID.randomUUID();
    private static final UUID NURSE_USER_ID = UUID.randomUUID();
    private static final UUID OFFER_ID = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID NURSE_ID = UUID.randomUUID();
    private static final UUID SERVICE_TYPE_ID = UUID.randomUUID();

    private static User patientUser() {
        return User.builder().id(PATIENT_USER_ID).build();
    }

    private static User nurseUser() {
        return User.builder().id(NURSE_USER_ID).profileImageUrl("nurse-avatar").build();
    }

    private static Profile patientProfile() {
        return Profile.builder().id(UUID.randomUUID()).user(patientUser()).build();
    }

    private static Nurse nurse() {
        return Nurse.builder().id(NURSE_ID).user(nurseUser()).verificationStatus(VerificationStatus.APPROVED).build();
    }

    private static ServiceType serviceType() {
        return ServiceType.builder()
                .id(SERVICE_TYPE_ID)
                .name("Nursing")
                .estimatedDurationMinutes(60)
                .build();
    }

    private static ServiceRequest searchingRequest() {
        return ServiceRequest.builder()
                .id(REQUEST_ID)
                .profile(patientProfile())
                .serviceType(serviceType())
                .status(ServiceRequestStatus.SEARCHING)
                .build();
    }

    private static NurseOffer pendingOffer(ServiceRequest request, Nurse nurse) {
        return NurseOffer.builder()
                .id(OFFER_ID)
                .serviceRequest(request)
                .nurse(nurse)
                .proposedPrice(new BigDecimal("100.00"))
                .proposedDate(LocalDate.of(2026, 8, 20))
                .proposedTime(LocalTime.of(10, 0))
                .message("I can help")
                .status(NurseOfferStatus.PENDING)
                .createdAt(LocalDateTime.of(2026, 8, 10, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 0))
                .build();
    }

    private static NurseOfferResponse baseResponse(NurseOffer offer) {
        return new NurseOfferResponse(
                offer.getId(),
                offer.getServiceRequest().getId(),
                new NurseSummaryResponse(offer.getNurse().getId(), "N", "Nurse", "avatar", new BigDecimal("4.5"), 10),
                offer.getProposedPrice(),
                offer.getProposedDate(),
                offer.getProposedTime(),
                offer.getMessage(),
                offer.getStatus(),
                0.0,
                "placeholder",
                0,
                offer.getCreatedAt(),
                offer.getUpdatedAt());
    }

    private void runEventsImmediately() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(afterCommitExecutor).execute(any(Runnable.class));
    }

    // ---------- create ----------

    @Test
    void createHappyPathPersistsOfferPushesEventAndNotifiesPatient() {
        ServiceRequest request = searchingRequest();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(nurse()));
        when(serviceRequestRepository.existsByNurse_IdAndIsDeletedFalseAndStatusIn(eq(NURSE_ID), any())).thenReturn(false);
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, SERVICE_TYPE_ID))
                .thenReturn(Optional.of(NurseService.builder().isActive(true).build()));
        when(nurseOfferRepository.existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalseAndStatus(
                REQUEST_ID, NURSE_USER_ID, NurseOfferStatus.PENDING)).thenReturn(false);

        NurseOffer created = pendingOffer(request, nurse());
        when(nurseOfferMapper.toEntity(any(NurseOfferRequest.class))).thenReturn(created);
        when(nurseOfferRepository.save(created)).thenReturn(created);
        when(nurseOfferMapper.toResponse(created)).thenAnswer(inv -> baseResponse(created));
        runEventsImmediately();

        NurseOfferRequest offerRequest = new NurseOfferRequest(
                REQUEST_ID, new BigDecimal("100.00"),
                LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), "I can help");

        NurseOfferResponse response = offerService.create(NURSE_USER_ID, offerRequest);

        assertThat(response.status()).isEqualTo(NurseOfferStatus.PENDING);
        assertThat(response.serviceTypeName()).isEqualTo("Nursing");
        assertThat(response.estimatedDurationMinutes()).isEqualTo(60);
        assertThat(created.getServiceRequest()).isEqualTo(request);
        assertThat(created.getNurse()).isNotNull();
        assertThat(created.getIsDeleted()).isFalse();
        verify(nurseOfferRepository).save(created);
        verify(messagingTemplate).convertAndSend(eq("/topic/reservation/" + REQUEST_ID),
                any(ReservationEvent.class));
        verify(notificationService).create(any());
    }

    @Test
    void createWithDistanceComputesDistanceFromPresence() {
        ServiceRequest request = searchingRequest();
        request.setLatitude(new BigDecimal("30.00000000"));
        request.setLongitude(new BigDecimal("31.00000000"));
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(nurse()));
        when(serviceRequestRepository.existsByNurse_IdAndIsDeletedFalseAndStatusIn(eq(NURSE_ID), any())).thenReturn(false);
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, SERVICE_TYPE_ID))
                .thenReturn(Optional.of(NurseService.builder().isActive(true).build()));
        when(nurseOfferRepository.existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalseAndStatus(
                REQUEST_ID, NURSE_USER_ID, NurseOfferStatus.PENDING)).thenReturn(false);
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID.toString()))
                .thenReturn(Optional.of(new Point(31.00000000, 30.00000000)));

        NurseOffer created = pendingOffer(request, nurse());
        when(nurseOfferMapper.toEntity(any(NurseOfferRequest.class))).thenReturn(created);
        when(nurseOfferRepository.save(created)).thenReturn(created);
        when(nurseOfferMapper.toResponse(created)).thenAnswer(inv -> baseResponse(created));

        NurseOfferResponse response = offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null));

        assertThat(response.distanceKm()).isEqualTo(0.0);
    }

    @Test
    void createWhenServiceRequestNotFoundThrows() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service request not found");
    }

    @Test
    void createWhenNurseProfileNotFoundThrows() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID))
                .thenReturn(Optional.of(searchingRequest()));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse profile not found");
    }

    @Test
    void createWhenNurseAlreadyAssignedThrows() {
        ServiceRequest request = searchingRequest();
        request.setNurse(nurse());
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(nurse()));

        assertThatThrownBy(() -> offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already has a nurse assigned");
    }

    @Test
    void createWhenRequestAcceptedThrows() {
        ServiceRequest request = searchingRequest();
        request.setStatus(ServiceRequestStatus.ACCEPTED);
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(nurse()));

        assertThatThrownBy(() -> offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already accepted by another nurse");
    }

    @Test
    void createWhenRequestCancelledThrows() {
        ServiceRequest request = searchingRequest();
        request.setStatus(ServiceRequestStatus.CANCELLED);
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(nurse()));

        assertThatThrownBy(() -> offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cancelled by the patient");
    }

    @Test
    void createWhenRequestCompletedThrows() {
        ServiceRequest request = searchingRequest();
        request.setStatus(ServiceRequestStatus.COMPLETED);
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(nurse()));

        assertThatThrownBy(() -> offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void createWhenRequestInOtherStatusThrowsWithDefaultReason() {
        ServiceRequest request = searchingRequest();
        request.setStatus(ServiceRequestStatus.PENDING);
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(nurse()));

        assertThatThrownBy(() -> offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer accepting offers (status: PENDING)");
    }

    @Test
    void createWhenNurseNotApprovedThrows() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID))
                .thenReturn(Optional.of(searchingRequest()));
        Nurse unverified = nurse();
        unverified.setVerificationStatus(VerificationStatus.UNDER_REVIEW);
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(unverified));

        assertThatThrownBy(() -> offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Nurse is not eligible to create offers");
    }

    @Test
    void createWhenNurseHasActiveVisitThrows() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID))
                .thenReturn(Optional.of(searchingRequest()));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(nurse()));
        when(serviceRequestRepository.existsByNurse_IdAndIsDeletedFalseAndStatusIn(eq(NURSE_ID), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active visit");
    }

    @Test
    void createWhenNurseServiceLinkMissingThrows() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID))
                .thenReturn(Optional.of(searchingRequest()));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(nurse()));
        when(serviceRequestRepository.existsByNurse_IdAndIsDeletedFalseAndStatusIn(eq(NURSE_ID), any())).thenReturn(false);
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, SERVICE_TYPE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not provide the requested service");
    }

    @Test
    void createWhenNurseServiceLinkInactiveThrows() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID))
                .thenReturn(Optional.of(searchingRequest()));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(nurse()));
        when(serviceRequestRepository.existsByNurse_IdAndIsDeletedFalseAndStatusIn(eq(NURSE_ID), any())).thenReturn(false);
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, SERVICE_TYPE_ID))
                .thenReturn(Optional.of(NurseService.builder().isActive(false).build()));

        assertThatThrownBy(() -> offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not provide the requested service");
    }

    @Test
    void createWhenNurseAlreadyHasPendingOfferThrows() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQUEST_ID))
                .thenReturn(Optional.of(searchingRequest()));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(nurse()));
        when(serviceRequestRepository.existsByNurse_IdAndIsDeletedFalseAndStatusIn(eq(NURSE_ID), any())).thenReturn(false);
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, SERVICE_TYPE_ID))
                .thenReturn(Optional.of(NurseService.builder().isActive(true).build()));
        when(nurseOfferRepository.existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalseAndStatus(
                REQUEST_ID, NURSE_USER_ID, NurseOfferStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> offerService.create(NURSE_USER_ID,
                new NurseOfferRequest(REQUEST_ID, new BigDecimal("100.00"),
                        LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already submitted an offer");
    }

    // ---------- listByServiceRequest ----------

    @Test
    void listByServiceRequestForOwnerMapsOffers() {
        ServiceRequest request = searchingRequest();
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQUEST_ID))
                .thenReturn(List.of(offer));
        when(nurseOfferMapper.toResponse(offer)).thenAnswer(inv -> baseResponse(offer));

        List<NurseOfferResponse> result = offerService.listByServiceRequest(REQUEST_ID, PATIENT_USER_ID);

        assertThat(result).singleElement().satisfies(r -> assertThat(r.id()).isEqualTo(OFFER_ID));
    }

@Test
    void listByServiceRequestForNurseWithOfferReturnsOnlyTheirOwnOffers() {
        ServiceRequest request = searchingRequest();
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.existsByUser_Id(NURSE_USER_ID)).thenReturn(true);
        when(nurseOfferRepository.existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalse(
                REQUEST_ID, NURSE_USER_ID)).thenReturn(true);
        NurseOffer ownOffer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalse(
                REQUEST_ID, NURSE_USER_ID)).thenReturn(List.of(ownOffer));
        when(nurseOfferMapper.toResponse(ownOffer)).thenAnswer(inv -> baseResponse(ownOffer));

        List<NurseOfferResponse> result = offerService.listByServiceRequest(REQUEST_ID, NURSE_USER_ID);

        assertThat(result).singleElement().satisfies(r -> assertThat(r.id()).isEqualTo(OFFER_ID));
        verify(nurseOfferRepository, never())
                .findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(any());
    }

    @Test
    void listByServiceRequestForNurseWithoutOfferThrows() {
        ServiceRequest request = searchingRequest();
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.existsByUser_Id(NURSE_USER_ID)).thenReturn(true);
        when(nurseOfferRepository.existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalse(
                REQUEST_ID, NURSE_USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> offerService.listByServiceRequest(REQUEST_ID, NURSE_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service request not found");
    }

    @Test
    void listNearbyByServiceRequestForNurseWithOfferIncludesDistanceOfOwnOffer() {
        ServiceRequest request = searchingRequest();
        request.setLatitude(new BigDecimal("30.00000000"));
        request.setLongitude(new BigDecimal("31.00000000"));
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.existsByUser_Id(NURSE_USER_ID)).thenReturn(true);
        when(nurseOfferRepository.existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalse(
                REQUEST_ID, NURSE_USER_ID)).thenReturn(true);
        NurseOffer ownOffer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalse(
                REQUEST_ID, NURSE_USER_ID)).thenReturn(List.of(ownOffer));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID.toString()))
                .thenReturn(Optional.of(new Point(31.00000000, 30.00000000)));

        List<NearbyNurseOfferResponse> result = offerService.listNearbyByServiceRequest(REQUEST_ID, NURSE_USER_ID);

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.id()).isEqualTo(OFFER_ID);
            assertThat(r.distanceKm()).isZero();
        });
    }

@Test
    void listByServiceRequestForUnauthorizedThrows() {
        ServiceRequest request = searchingRequest();
        UUID strangerId = UUID.randomUUID();
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.existsByUser_Id(strangerId)).thenReturn(false);

        assertThatThrownBy(() -> offerService.listByServiceRequest(REQUEST_ID, strangerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service request not found");
    }

    @Test
    void listByServiceRequestWhenRequestMissingThrows() {
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.listByServiceRequest(REQUEST_ID, PATIENT_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service request not found");
    }

    // ---------- listNearbyByServiceRequest ----------

    @Test
    void listNearbyFiltersByRadiusAndSortsByDistance() {
        ServiceRequest request = searchingRequest();
        request.setLatitude(new BigDecimal("30.00000000"));
        request.setLongitude(new BigDecimal("31.00000000"));
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        NurseOffer closeOffer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQUEST_ID))
                .thenReturn(List.of(closeOffer));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID.toString()))
                .thenReturn(Optional.of(new Point(31.00000000, 30.00000000)));

        List<NearbyNurseOfferResponse> result = offerService.listNearbyByServiceRequest(REQUEST_ID, PATIENT_USER_ID);

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.distanceKm()).isZero();
            assertThat(r.estimatedDurationMinutes()).isEqualTo(60);
        });
    }

    @Test
    void listNearbyKeepsOnlyOffersWithinRadiusAndDropsNursesWithoutLocation() {
        ServiceRequest request = searchingRequest();
        request.setLatitude(new BigDecimal("30.00000000"));
        request.setLongitude(new BigDecimal("31.00000000"));
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        NurseOffer farOffer = pendingOffer(request, nurse());
        UUID otherNurseUserId = UUID.randomUUID();
        NurseOffer noLocationOffer = NurseOffer.builder()
                .id(UUID.randomUUID())
                .serviceRequest(request)
                .nurse(Nurse.builder().id(UUID.randomUUID()).user(User.builder().id(otherNurseUserId).build()).build())
                .status(NurseOfferStatus.PENDING)
                .build();
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQUEST_ID))
                .thenReturn(List.of(farOffer, noLocationOffer));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID.toString()))
                .thenReturn(Optional.of(new Point(40.00000000, 30.00000000)));
        when(webSocketPresenceService.getAvailableLocation(otherNurseUserId.toString()))
                .thenReturn(Optional.empty());

        List<NearbyNurseOfferResponse> result = offerService.listNearbyByServiceRequest(REQUEST_ID, PATIENT_USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void listNearbyWithNoLocationThrows() {
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(searchingRequest()));

        assertThatThrownBy(() -> offerService.listNearbyByServiceRequest(REQUEST_ID, PATIENT_USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("location is unavailable");
    }

    @Test
    void listNearbyWithOnlyLatitudeThrows() {
        ServiceRequest request = searchingRequest();
        request.setLatitude(new BigDecimal("30.00000000"));
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> offerService.listNearbyByServiceRequest(REQUEST_ID, PATIENT_USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("location is unavailable");
    }

    @Test
    void listNearbyWithServiceTypeNullUsesNullDuration() {
        ServiceRequest request = searchingRequest();
        request.setServiceType(null);
        request.setLatitude(new BigDecimal("30.00000000"));
        request.setLongitude(new BigDecimal("31.00000000"));
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQUEST_ID))
                .thenReturn(List.of(offer));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID.toString()))
                .thenReturn(Optional.of(new Point(31.00000000, 30.00000000)));

        List<NearbyNurseOfferResponse> result = offerService.listNearbyByServiceRequest(REQUEST_ID, PATIENT_USER_ID);

        assertThat(result).singleElement().satisfies(r -> assertThat(r.estimatedDurationMinutes()).isNull());
    }

    // ---------- get ----------

    @Test
    void getAuthorizedOfferReturnsResponseWithServiceTypeName() {
        ServiceRequest request = searchingRequest();
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferMapper.toResponse(offer)).thenAnswer(inv -> baseResponse(offer));

        NurseOfferResponse response = offerService.get(OFFER_ID, PATIENT_USER_ID);

        assertThat(response.id()).isEqualTo(OFFER_ID);
        assertThat(response.serviceTypeName()).isEqualTo("Nursing");
    }

    @Test
    void getReturnsNullServiceTypeDetailsWhenRequestHasNoServiceType() {
        ServiceRequest request = searchingRequest();
        request.setServiceType(null);
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferMapper.toResponse(offer)).thenAnswer(inv -> baseResponse(offer));

        NurseOfferResponse response = offerService.get(OFFER_ID, PATIENT_USER_ID);

        assertThat(response.serviceTypeName()).isNull();
        assertThat(response.estimatedDurationMinutes()).isNull();
        assertThat(response.distanceKm()).isNull();
    }

    @Test
    void getAddsDistanceWhenLocationAvailable() {
        ServiceRequest request = searchingRequest();
        request.setLatitude(new BigDecimal("30.00000000"));
        request.setLongitude(new BigDecimal("31.00000000"));
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferMapper.toResponse(offer)).thenAnswer(inv -> baseResponse(offer));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID.toString()))
                .thenReturn(Optional.of(new Point(31.00000000, 30.00000000)));

        NurseOfferResponse response = offerService.get(OFFER_ID, PATIENT_USER_ID);

        assertThat(response.distanceKm()).isZero();
    }

    @Test
    void getWhenPresenceFailsReturnsNullDistance() {
        ServiceRequest request = searchingRequest();
        request.setLatitude(new BigDecimal("30.00000000"));
        request.setLongitude(new BigDecimal("31.00000000"));
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferMapper.toResponse(offer)).thenAnswer(inv -> baseResponse(offer));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID.toString()))
                .thenThrow(new RuntimeException("redis down"));

        NurseOfferResponse response = offerService.get(OFFER_ID, PATIENT_USER_ID);

        assertThat(response.distanceKm()).isNull();
    }

    @Test
    void getWhenUnAuthorizedThrows() {
        NurseOffer offer = pendingOffer(searchingRequest(), nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.get(OFFER_ID, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse offer not found");
    }

    @Test
    void getWhenOfferMissingThrows() {
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.get(OFFER_ID, PATIENT_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse offer not found");
    }

    // ---------- accept ----------

    @Test
    void acceptByPatientAcceptsOfferRejectsOthersAndNotifies() {
        ServiceRequest request = searchingRequest();
        Nurse offeringNurse = nurse();
        NurseOffer offer = pendingOffer(request, offeringNurse);
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        NurseOffer otherPending = NurseOffer.builder()
                .id(UUID.randomUUID())
                .serviceRequest(request)
                .nurse(Nurse.builder().id(UUID.randomUUID()).user(nurseUser()).build())
                .status(NurseOfferStatus.PENDING)
                .build();
        NurseOffer otherRejected = NurseOffer.builder()
                .id(UUID.randomUUID())
                .serviceRequest(request)
                .nurse(Nurse.builder().id(UUID.randomUUID()).user(nurseUser()).build())
                .status(NurseOfferStatus.REJECTED)
                .build();
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQUEST_ID))
                .thenReturn(List.of(offer, otherPending, otherRejected));
        when(nurseOfferRepository.save(offer)).thenReturn(offer);
        when(nurseOfferMapper.toResponse(offer)).thenAnswer(inv -> baseResponse(offer));
        runEventsImmediately();

        NurseOfferResponse response = offerService.accept(OFFER_ID, PATIENT_USER_ID);

        assertThat(response.status()).isEqualTo(NurseOfferStatus.ACCEPTED);
        assertThat(request.getNurse()).isEqualTo(offeringNurse);
        assertThat(request.getStatus()).isEqualTo(ServiceRequestStatus.ACCEPTED);
        assertThat(otherPending.getStatus()).isEqualTo(NurseOfferStatus.REJECTED);
        assertThat(otherRejected.getStatus()).isEqualTo(NurseOfferStatus.REJECTED);
        verify(nurseOfferRepository).save(offer);
        verify(messagingTemplate, org.mockito.Mockito.times(2))
                .convertAndSend(eq("/topic/reservation/" + REQUEST_ID), any(ReservationEvent.class));
        verify(notificationService, org.mockito.Mockito.times(3)).create(any());
    }

    @Test
    void acceptByOfferingNurseIsAllowed() {
        ServiceRequest request = searchingRequest();
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQUEST_ID))
                .thenReturn(List.of(offer));
        when(nurseOfferRepository.save(offer)).thenReturn(offer);
        when(nurseOfferMapper.toResponse(offer)).thenAnswer(inv -> baseResponse(offer));

        NurseOfferResponse response = offerService.accept(OFFER_ID, NURSE_USER_ID);

        assertThat(response.status()).isEqualTo(NurseOfferStatus.ACCEPTED);
    }

    @Test
    void getWhenOnlyLatitudeSetReturnsNullDistance() {
        ServiceRequest request = searchingRequest();
        request.setLatitude(new BigDecimal("30.00000000"));
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferMapper.toResponse(offer)).thenReturn(baseResponse(offer));

        NurseOfferResponse response = offerService.get(OFFER_ID, PATIENT_USER_ID);

        assertThat(response.distanceKm()).isNull();
    }

    @Test
    void acceptWhenNurseHasNoUserSkipsNurseNotification() {
        ServiceRequest request = searchingRequest();
        Nurse offerNurse = org.mockito.Mockito.mock(Nurse.class);
        when(offerNurse.getUser()).thenReturn(nurseUser(), (User) null);
        when(offerNurse.getId()).thenReturn(NURSE_ID);
        NurseOffer offer = pendingOffer(request, offerNurse);
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQUEST_ID))
                .thenReturn(List.of(offer));
        when(nurseOfferRepository.save(offer)).thenReturn(offer);
        when(nurseOfferMapper.toResponse(offer)).thenAnswer(inv -> baseResponse(offer));

        offerService.accept(OFFER_ID, PATIENT_USER_ID);

        verify(notificationService).create(any());
    }

    @Test
    void acceptWhenOfferNurseNullSkipsNurseNotificationBlock() {
        ServiceRequest request = searchingRequest();
        NurseOffer mockOffer = org.mockito.Mockito.mock(NurseOffer.class);
        when(mockOffer.getServiceRequest()).thenReturn(request);
        when(mockOffer.getId()).thenReturn(OFFER_ID);
        when(mockOffer.getStatus()).thenReturn(NurseOfferStatus.PENDING);
        when(mockOffer.getNurse()).thenReturn(nurse(), null);
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(mockOffer));
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQUEST_ID))
                .thenReturn(List.of(mockOffer));
        when(nurseOfferRepository.save(mockOffer)).thenReturn(mockOffer);
        when(nurseOfferMapper.toResponse(mockOffer)).thenReturn(new NurseOfferResponse(
                OFFER_ID, REQUEST_ID,
                new NurseSummaryResponse(NURSE_ID, "N", "Nurse", "avatar", new BigDecimal("4.5"), 10),
                new BigDecimal("100.00"), LocalDate.of(2026, 8, 20), LocalTime.of(10, 0), "I can help",
                NurseOfferStatus.PENDING, null, "Nursing", 60,
                LocalDateTime.of(2026, 8, 10, 9, 0), LocalDateTime.of(2026, 8, 10, 9, 0)));
        runEventsImmediately();

        offerService.accept(OFFER_ID, PATIENT_USER_ID);

        verify(notificationService).create(any());
    }

    @Test
    void acceptWhenOfferMissingThrows() {
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.accept(OFFER_ID, PATIENT_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse offer not found");
    }

    @Test
    void acceptWhenNeitherPatientNorNurseThrows() {
        NurseOffer offer = pendingOffer(searchingRequest(), nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.accept(OFFER_ID, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse offer not found");
    }

    @Test
    void acceptWhenOfferNotPendingThrows() {
        NurseOffer offer = pendingOffer(searchingRequest(), nurse());
        offer.setStatus(NurseOfferStatus.ACCEPTED);
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.accept(OFFER_ID, PATIENT_USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only pending offers can be accepted");
    }

@Test
    void acceptWhenNurseAlreadySelectedThrows() {
        ServiceRequest request = searchingRequest();
        request.setNurse(Nurse.builder().id(UUID.randomUUID()).build());
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.accept(OFFER_ID, PATIENT_USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been selected");
    }

    // ---------- update ----------

    @Test
    void updateAppliesAllFieldsAndNotifies() {
        ServiceRequest request = searchingRequest();
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferRepository.save(offer)).thenReturn(offer);
        when(nurseOfferMapper.toResponse(offer)).thenAnswer(inv -> baseResponse(offer));
        runEventsImmediately();

        NurseOfferResponse response = offerService.update(OFFER_ID, NURSE_USER_ID,
                new NurseOfferUpdateRequest(new BigDecimal("150.00"),
                        LocalDate.of(2026, 8, 21), LocalTime.of(11, 30), "new terms"));

        assertThat(response.proposedPrice()).isEqualTo(new BigDecimal("150.00"));
        assertThat(offer.getProposedDate()).isEqualTo(LocalDate.of(2026, 8, 21));
        assertThat(offer.getProposedTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(offer.getMessage()).isEqualTo("new terms");
        verify(messagingTemplate).convertAndSend(eq("/topic/reservation/" + REQUEST_ID),
                any(ReservationEvent.class));
    }

    @Test
    void updateWithNullFieldsKeepsExistingValues() {
        ServiceRequest request = searchingRequest();
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferRepository.save(offer)).thenReturn(offer);
        when(nurseOfferMapper.toResponse(offer)).thenAnswer(inv -> baseResponse(offer));

        offerService.update(OFFER_ID, NURSE_USER_ID,
                new NurseOfferUpdateRequest(null, null, null, null));

        assertThat(offer.getProposedPrice()).isEqualTo(new BigDecimal("100.00"));
        assertThat(offer.getMessage()).isEqualTo("I can help");
    }

    @Test
    void updateWhenNotOwnedByNurseThrows() {
        NurseOffer offer = pendingOffer(searchingRequest(), nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.update(OFFER_ID, PATIENT_USER_ID,
                new NurseOfferUpdateRequest(new BigDecimal("150.00"), null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse offer not found");
    }

    @Test
    void updateWhenOfferMissingThrows() {
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.update(OFFER_ID, NURSE_USER_ID,
                new NurseOfferUpdateRequest(null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse offer not found");
    }

    @Test
    void updateWhenOfferNotPendingThrows() {
        NurseOffer offer = pendingOffer(searchingRequest(), nurse());
        offer.setStatus(NurseOfferStatus.WITHDRAWN);
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.update(OFFER_ID, NURSE_USER_ID,
                new NurseOfferUpdateRequest(new BigDecimal("150.00"), null, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only pending offers can be updated");
    }

    // ---------- counterOffer ----------

    @Test
    void counterOfferByPatientAppliesTermsAndNotifiesNurse() {
        ServiceRequest request = searchingRequest();
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferRepository.save(offer)).thenReturn(offer);
        when(nurseOfferMapper.toResponse(offer)).thenAnswer(inv -> baseResponse(offer));
        runEventsImmediately();

        NurseOfferResponse response = offerService.counterOffer(OFFER_ID, PATIENT_USER_ID,
                new NurseOfferUpdateRequest(new BigDecimal("90.00"),
                        LocalDate.of(2026, 8, 22), LocalTime.of(9, 30), "counter"));

        assertThat(response.proposedPrice()).isEqualTo(new BigDecimal("90.00"));
        assertThat(offer.getProposedDate()).isEqualTo(LocalDate.of(2026, 8, 22));
        assertThat(offer.getProposedTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(offer.getMessage()).isEqualTo("counter");
        verify(notificationService).create(any());
    }

    @Test
    void counterOfferWhenNotOwnerThrows() {
        NurseOffer offer = pendingOffer(searchingRequest(), nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.counterOffer(OFFER_ID, NURSE_USER_ID,
                new NurseOfferUpdateRequest(new BigDecimal("90.00"), null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse offer not found");
    }

    @Test
    void counterOfferWhenOfferNotPendingThrows() {
        NurseOffer offer = pendingOffer(searchingRequest(), nurse());
        offer.setStatus(NurseOfferStatus.ACCEPTED);
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.counterOffer(OFFER_ID, PATIENT_USER_ID,
                new NurseOfferUpdateRequest(new BigDecimal("90.00"), null, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only pending offers can be countered");
    }

    @Test
    void counterOfferWithNullFieldsKeepsExistingValues() {
        ServiceRequest request = searchingRequest();
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferRepository.save(offer)).thenReturn(offer);
        when(nurseOfferMapper.toResponse(offer)).thenAnswer(inv -> baseResponse(offer));

        offerService.counterOffer(OFFER_ID, PATIENT_USER_ID,
                new NurseOfferUpdateRequest(null, null, null, null));

        assertThat(offer.getProposedPrice()).isEqualTo(new BigDecimal("100.00"));
        assertThat(offer.getProposedDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(offer.getProposedTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(offer.getMessage()).isEqualTo("I can help");
    }

    // ---------- reject ----------

    @Test
    void rejectByPatientMarksRejectedAndNotifiesNurse() {
        ServiceRequest request = searchingRequest();
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferRepository.save(offer)).thenReturn(offer);
        runEventsImmediately();

        offerService.reject(OFFER_ID, PATIENT_USER_ID);

        assertThat(offer.getStatus()).isEqualTo(NurseOfferStatus.REJECTED);
        verify(nurseOfferRepository).save(offer);
        verify(notificationService).create(any());
    }

    @Test
    void rejectWhenNotOwnerThrows() {
        NurseOffer offer = pendingOffer(searchingRequest(), nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.reject(OFFER_ID, NURSE_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse offer not found");
    }

    @Test
    void rejectWhenOfferNotPendingThrows() {
        NurseOffer offer = pendingOffer(searchingRequest(), nurse());
        offer.setStatus(NurseOfferStatus.REJECTED);
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.reject(OFFER_ID, PATIENT_USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only pending offers can be rejected");
    }

    // ---------- withdraw ----------

    @Test
    void withdrawByNurseMarksWithdrawnAndNotifiesPatient() {
        ServiceRequest request = searchingRequest();
        NurseOffer offer = pendingOffer(request, nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));
        when(nurseOfferRepository.save(offer)).thenReturn(offer);
        runEventsImmediately();

        offerService.withdraw(OFFER_ID, NURSE_USER_ID);

        assertThat(offer.getStatus()).isEqualTo(NurseOfferStatus.WITHDRAWN);
        verify(nurseOfferRepository).save(offer);
        verify(notificationService).create(any());
    }

    @Test
    void withdrawWhenNotOwnedByNurseThrows() {
        NurseOffer offer = pendingOffer(searchingRequest(), nurse());
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.withdraw(OFFER_ID, PATIENT_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Nurse offer not found");
    }

    @Test
    void withdrawWhenOfferNotPendingThrows() {
        NurseOffer offer = pendingOffer(searchingRequest(), nurse());
        offer.setStatus(NurseOfferStatus.WITHDRAWN);
        when(nurseOfferRepository.findByIdAndIsDeletedFalse(OFFER_ID)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.withdraw(OFFER_ID, NURSE_USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only pending offers can be withdrawn");
    }

    // ---------- getAuthorizedServiceRequest via listNearby ----------

@Test
    void getAuthorizedServiceRequestAllowsAssignedNurse() {
        ServiceRequest request = searchingRequest();
        request.setNurse(nurse());
        request.setLatitude(new BigDecimal("30.00000000"));
        request.setLongitude(new BigDecimal("31.00000000"));
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.existsByUser_Id(NURSE_USER_ID)).thenReturn(true);
        when(nurseOfferRepository.existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalse(
                REQUEST_ID, NURSE_USER_ID)).thenReturn(true);
        when(nurseOfferRepository.findByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalse(
                REQUEST_ID, NURSE_USER_ID)).thenReturn(List.of());

        List<NearbyNurseOfferResponse> result = offerService.listNearbyByServiceRequest(REQUEST_ID, NURSE_USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getAuthorizedServiceRequestRejectsNurseWithoutProfile() {
        ServiceRequest request = searchingRequest();
        request.setNurse(nurse());
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.existsByUser_Id(NURSE_USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> offerService.listNearbyByServiceRequest(REQUEST_ID, NURSE_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service request not found");
    }

    @Test
    void getAuthorizedServiceRequestRejectsOwnerOfOtherRequest() {
        UUID otherPatientId = UUID.randomUUID();
        ServiceRequest request = ServiceRequest.builder()
                .id(REQUEST_ID)
                .profile(Profile.builder().id(UUID.randomUUID()).user(User.builder().id(otherPatientId).build()).build())
                .serviceType(serviceType())
                .status(ServiceRequestStatus.SEARCHING)
                .build();
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.existsByUser_Id(PATIENT_USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> offerService.listNearbyByServiceRequest(REQUEST_ID, PATIENT_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service request not found");
    }

    @Test
    void getAuthorizedServiceRequestRejectsNurseNotAssignedToRequest() {
        ServiceRequest request = searchingRequest();
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.existsByUser_Id(NURSE_USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> offerService.listNearbyByServiceRequest(REQUEST_ID, NURSE_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service request not found");
    }

    @Test
    void getAuthorizedServiceRequestRejectsNurseAssignedToOtherUser() {
        ServiceRequest request = searchingRequest();
        request.setNurse(Nurse.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(UUID.randomUUID()).build())
                .build());
        when(serviceRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(nurseRepository.existsByUser_Id(NURSE_USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> offerService.listNearbyByServiceRequest(REQUEST_ID, NURSE_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service request not found");
    }

    // ---------- notifyPatient guard via reflection ----------

    @Test
    void notifyPatientSkipsWhenServiceRequestNull() {
        ReflectionTestUtils.invokeMethod(offerService, "notifyPatient", (Object) null, "t", "m");

        verify(notificationService, never()).create(any());
    }

    @Test
    void notifyPatientSkipsWhenProfileNull() {
        ReflectionTestUtils.invokeMethod(offerService, "notifyPatient",
                ServiceRequest.builder().id(REQUEST_ID).build(), "t", "m");

        verify(notificationService, never()).create(any());
    }

    @Test
    void notifyPatientSkipsWhenUserNull() {
        ServiceRequest request = ServiceRequest.builder()
                .id(REQUEST_ID)
                .profile(Profile.builder().id(UUID.randomUUID()).build())
                .build();
        ReflectionTestUtils.invokeMethod(offerService, "notifyPatient", request, "t", "m");

        verify(notificationService, never()).create(any());
    }

    @Test
    void notifyPatientCreatesNotificationForPatient() {
        ServiceRequest request = searchingRequest();
        ReflectionTestUtils.invokeMethod(offerService, "notifyPatient", request, "title", "message");

        verify(notificationService).create(any());
    }
}
