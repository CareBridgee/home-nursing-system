package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.dto.nurse.NearbyNurse;
import iti.jets.java.homenursing.dto.nurse.NurseLocation;
import iti.jets.java.homenursing.dto.nurse.NurseSummaryResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.dto.reservation.ReservationEvent;
import iti.jets.java.homenursing.dto.servicerequest.NearbyNurseServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestRequest;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.NurseRequestHistoryResponse;
import iti.jets.java.homenursing.dto.servicerequest.PatientMedicalSummary;
import iti.jets.java.homenursing.dto.servicerequest.ServiceRequestDetailsResponse;
import iti.jets.java.homenursing.dto.servicerequest.ServiceRequestHistoryResponse;
import iti.jets.java.homenursing.dto.servicerequest.ServiceRequestNursePreviewResponse;
import iti.jets.java.homenursing.dto.servicerequest.ServiceRequestNurseProfileResponse;
import iti.jets.java.homenursing.dto.servicerequest.VisitCodeResponse;
import iti.jets.java.homenursing.entity.Address;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseOffer;
import iti.jets.java.homenursing.entity.NurseService;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.Gender;
import iti.jets.java.homenursing.entity.enums.NotificationType;
import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ForbiddenException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.NurseOfferMapper;
import iti.jets.java.homenursing.repository.AddressRepository;
import iti.jets.java.homenursing.repository.NurseOfferRepository;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.NurseServiceRepository;
import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.service.impl.NearbyNurseMatcherImpl;
import iti.jets.java.homenursing.service.impl.NurseLocationProviderImpl;
import iti.jets.java.homenursing.service.impl.ServiceRequestServiceImpl;
import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import iti.jets.java.homenursing.util.AfterCommitExecutor;
import iti.jets.java.homenursing.util.PatientMedicalSummaryAssembler;
import iti.jets.java.homenursing.util.PriceEstimator;
import iti.jets.java.homenursing.util.ServiceBriefBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.geo.Point;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceRequestServiceImplTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ServiceTypeRepository serviceTypeRepository;
    @Mock
    private NurseOfferRepository nurseOfferRepository;
    @Mock
    private NurseRepository nurseRepository;
    @Mock
    private NurseServiceRepository nurseServiceRepository;
    @Mock
    private NurseOfferMapper nurseOfferMapper;
    @Mock
    private ProfileService profileService;
    @Mock
    private WebSocketPresenceService webSocketPresenceService;
    @Mock
    private TokenService tokenService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private AfterCommitExecutor afterCommitExecutor;
    @Mock
    private PatientMedicalSummaryAssembler patientMedicalSummaryAssembler;
    @Mock
    private AddressRepository addressRepository;

    private ServiceRequestServiceImpl service;
    private PriceEstimator priceEstimator;
    private NearbyNurseMatcherImpl nearbyNurseMatcher;
    private NurseLocationProviderImpl nurseLocationProvider;
    private ServiceBriefBuilder serviceBriefBuilder;

    private static final UUID REQ_ID = UUID.randomUUID();
    private static final UUID REQ_ID_2 = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID ST_ID = UUID.randomUUID();
    private static final UUID ST_ID_2 = UUID.randomUUID();
    private static final UUID NURSE_ID = UUID.randomUUID();
    private static final UUID NURSE_ID_2 = UUID.randomUUID();
    private static final UUID NURSE_USER_ID = UUID.randomUUID();
    private static final UUID NURSE_USER_ID_2 = UUID.randomUUID();
    private static final UUID PATIENT_USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID OFFER_ID = UUID.randomUUID();
    private static final String NURSE_USER_ID_STR = NURSE_USER_ID.toString();
    private static final String NURSE_USER_ID_2_STR = NURSE_USER_ID_2.toString();
    private static final BigDecimal LAT = new BigDecimal("30.0000");
    private static final BigDecimal LNG = new BigDecimal("31.0000");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 10, 0);
    private static final LocalDateTime NOW_2 = NOW.plusHours(1);
    private static final BigDecimal BASE_PRICE = new BigDecimal("500");

    private static final PatientMedicalSummary SUMMARY = new PatientMedicalSummary(
            PROFILE_ID, "Mona", "Ali", "patient-img",
            LocalDate.of(1996, 6, 15), Gender.MALE, "A+",
            new BigDecimal("170"), new BigDecimal("70"), "mobile", null, null, null,
            List.of("Penicillin"), List.of("Hypertension"), List.of("Aspirin"),
            List.of(), List.of());

    @BeforeEach
    void setUp() {
        priceEstimator = new PriceEstimator();
        ReflectionTestUtils.setField(priceEstimator, "includedDistanceKm", 5.0);
        ReflectionTestUtils.setField(priceEstimator, "pricePerKm", new BigDecimal("12"));

        nearbyNurseMatcher = new NearbyNurseMatcherImpl();
        ReflectionTestUtils.setField(nearbyNurseMatcher, "defaultRadiusKm", 10.0);

        nurseLocationProvider = new NurseLocationProviderImpl(webSocketPresenceService, nurseRepository);
        serviceBriefBuilder = new ServiceBriefBuilder(profileService, patientMedicalSummaryAssembler);

        service = new ServiceRequestServiceImpl(
                serviceRequestRepository, serviceTypeRepository, nurseOfferRepository, nurseRepository,
                nurseServiceRepository, nurseOfferMapper, profileService, nurseLocationProvider, nearbyNurseMatcher,
                priceEstimator, webSocketPresenceService, tokenService, notificationService, messagingTemplate,
                afterCommitExecutor, patientMedicalSummaryAssembler, serviceBriefBuilder, addressRepository);

        ReflectionTestUtils.setField(service, "nearbyNursesRadiusKm", 10.0);
        ReflectionTestUtils.setField(service, "visitCodeTtlHours", 24L);
        ReflectionTestUtils.setField(service, "visitCodeMaxAttempts", 5);

        when(patientMedicalSummaryAssembler.build(any(Profile.class), anyBoolean())).thenReturn(SUMMARY);
    }

    private void runAfterCommitEvents() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(afterCommitExecutor).execute(any(Runnable.class));
    }

    private User patientUser() {
        return User.builder()
                .id(PATIENT_USER_ID)
                .phoneNumber("01012345678")
                .firstName("Mona")
                .lastName("Ali")
                .profileImageUrl("user-img")
                .build();
    }

    private User nurseUser() {
        return User.builder()
                .id(NURSE_USER_ID)
                .phoneNumber("01112345678")
                .firstName("Sara")
                .lastName("Hassan")
                .profileImageUrl("nurse-img")
                .build();
    }

    private Profile patientProfile() {
        return Profile.builder()
                .id(PROFILE_ID)
                .user(patientUser())
                .firstName("Mona")
                .lastName("Ali")
                .profileImageUrl("patient-img")
                .build();
    }

    private Nurse assignedNurse() {
        return Nurse.builder()
                .id(NURSE_ID)
                .user(nurseUser())
                .verificationStatus(VerificationStatus.APPROVED)
                .ratingAvg(new BigDecimal("4.8"))
                .totalReviews(10)
                .build();
    }

    private Nurse nurseWithUserId(UUID id, UUID userId) {
        return Nurse.builder()
                .id(id)
                .user(User.builder().id(userId).firstName("N" + userId).lastName("L").build())
                .verificationStatus(VerificationStatus.APPROVED)
                .build();
    }

    private ServiceType serviceType() {
        return ServiceType.builder()
                .id(ST_ID)
                .name("Home Nursing")
                .basePrice(BASE_PRICE)
                .estimatedDurationMinutes(120)
                .build();
    }

    private ServiceType serviceType2() {
        return ServiceType.builder()
                .id(ST_ID_2)
                .name("Physiotherapy")
                .basePrice(new BigDecimal("600"))
                .estimatedDurationMinutes(60)
                .build();
    }

    private NurseService nurseService(Nurse nurse, ServiceType serviceType) {
        return NurseService.builder()
                .id(UUID.randomUUID())
                .nurse(nurse)
                .serviceType(serviceType)
                .isActive(true)
                .build();
    }

    private ServiceRequest request() {
        return ServiceRequest.builder()
                .id(REQ_ID)
                .profile(patientProfile())
                .serviceType(serviceType())
                .serviceDescription("In-home nursing care")
                .preferredDate(LocalDate.now().plusDays(2))
                .preferredTime(LocalTime.of(9, 0))
                .durationMinutes(90)
                .status(ServiceRequestStatus.SEARCHING)
                .latitude(LAT)
                .longitude(LNG)
                .isDeleted(false)
                .createdAt(NOW)
                .updatedAt(NOW_2)
                .build();
    }

    private ServiceRequest requestWithNurse() {
        return ServiceRequest.builder()
                .id(REQ_ID)
                .profile(patientProfile())
                .serviceType(serviceType())
                .nurse(assignedNurse())
                .serviceDescription("In-home nursing care")
                .preferredDate(LocalDate.now().plusDays(2))
                .preferredTime(LocalTime.of(9, 0))
                .durationMinutes(90)
                .status(ServiceRequestStatus.ACCEPTED)
                .latitude(LAT)
                .longitude(LNG)
                .isDeleted(false)
                .createdAt(NOW)
                .updatedAt(NOW_2)
                .build();
    }

    private NurseOffer offer(UUID id, SharedOffer nurse, NurseOfferStatus status) {
        return NurseOffer.builder()
                .id(id)
                .serviceRequest(request())
                .nurse(nurse == null ? null : nurse.nurse())
                .proposedPrice(new BigDecimal("350"))
                .proposedDate(LocalDate.now().plusDays(2))
                .proposedTime(LocalTime.of(10, 0))
                .message("I can help")
                .status(status)
                .isDeleted(false)
                .createdAt(NOW)
                .updatedAt(NOW_2)
                .build();
    }

    private record SharedOffer(Nurse nurse) {
    }

    private Address address() {
        return Address.builder()
                .id(UUID.randomUUID())
                .profile(patientProfile())
                .country("Egypt")
                .city("Cairo")
                .area("Zamalek")
                .street("Tahrir")
                .buildingNumber("12")
                .apartmentNumber("3B")
                .build();
    }

    private void stubTwoNearbyNurses() {
        when(webSocketPresenceService.getOnlineNurses())
                .thenReturn(Set.of(NURSE_USER_ID_STR, NURSE_USER_ID_2_STR));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID))
                .thenReturn(Optional.of(assignedNurse()));
        when(nurseRepository.findByUser_Id(NURSE_USER_ID_2))
                .thenReturn(Optional.of(nurseWithUserId(NURSE_ID_2, NURSE_USER_ID_2)));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.0000, 30.0000)));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_2_STR))
                .thenReturn(Optional.of(new Point(31.0100, 30.0050)));
        when(nurseServiceRepository.findByServiceType_IdAndIsActiveTrue(ST_ID))
                .thenReturn(List.of(nurseService(assignedNurse(), serviceType()),
                        nurseService(nurseWithUserId(NURSE_ID_2, NURSE_USER_ID_2), serviceType())));
    }

    private void stubSaveAndFlush() {
        when(serviceRequestRepository.saveAndFlush(any(ServiceRequest.class)))
                .thenAnswer(invocation -> {
                    ServiceRequest sr = invocation.getArgument(0);
                    sr.setId(REQ_ID);
                    sr.setCreatedAt(NOW);
                    return sr;
                });
    }

    private String visitCodeKey() {
        return "visitcode:" + REQ_ID;
    }

    private String visitCodeAttemptsKey() {
        return "visitcode_attempts:" + REQ_ID;
    }

    // ------------------------------------------------------------------
    // createRequest
    // ------------------------------------------------------------------

    @Test
    void createRequest_happyPath_returnsResponseWithSortedNearbyNurses() {
        when(profileService.getProfile(PROFILE_ID)).thenReturn(patientProfile());
        when(serviceTypeRepository.findById(ST_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceRequestRepository.existsByProfile_IdAndIsDeletedFalseAndStatusIn(
                PROFILE_ID, Set.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.SEARCHING,
                        ServiceRequestStatus.BOOKING, ServiceRequestStatus.NEGOTIATING,
                        ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.IN_PROGRESS)))
                .thenReturn(false);
        stubTwoNearbyNurses();
        stubSaveAndFlush();

        NearbyServiceRequestResponse response = service.createRequest(new NearbyServiceRequestRequest(
                PROFILE_ID, ST_ID, LAT, LNG, LocalDate.now().plusDays(1), LocalTime.of(10, 0),
                "Specialized home care", null));

        assertEquals(REQ_ID, response.serviceRequestId());
        assertEquals(PROFILE_ID, response.profileId());
        assertEquals(ST_ID, response.serviceTypeId());
        assertEquals(ServiceRequestStatus.SEARCHING, response.status());
        assertEquals(LAT, response.latitude());
        assertEquals(LNG, response.longitude());
        assertEquals(NOW, response.createdAt());
        assertEquals(2, response.nearbyNurses().size());
        assertEquals(NURSE_ID, response.nearbyNurses().get(0).nurseId());
        assertEquals(0.0, response.nearbyNurses().get(0).distanceKm());
        assertTrue(response.nearbyNurses().get(1).distanceKm() > 0);

        ArgumentCaptor<ServiceRequest> captor = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(serviceRequestRepository).saveAndFlush(captor.capture());
        assertEquals("Specialized home care", captor.getValue().getServiceDescription());
        assertEquals(ServiceRequestStatus.SEARCHING, captor.getValue().getStatus());
        assertEquals(patientProfile().getId(), captor.getValue().getProfile().getId());
        assertFalse(Boolean.TRUE.equals(captor.getValue().getIsDeleted()));
    }

    @Test
    void createRequest_pastPreferredDate_throws() {
        when(profileService.getProfile(PROFILE_ID)).thenReturn(patientProfile());
        when(serviceTypeRepository.findById(ST_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceRequestRepository.existsByProfile_IdAndIsDeletedFalseAndStatusIn(eq(PROFILE_ID), anyCollection()))
                .thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.createRequest(new NearbyServiceRequestRequest(
                        PROFILE_ID, ST_ID, LAT, LNG, LocalDate.now().minusDays(1), null, null, null)));

        assertEquals("Preferred date must not be in the past", ex.getMessage());
    }

    @Test
    void createRequest_pastPreferredTimeToday_throws() {
        when(profileService.getProfile(PROFILE_ID)).thenReturn(patientProfile());
        when(serviceTypeRepository.findById(ST_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceRequestRepository.existsByProfile_IdAndIsDeletedFalseAndStatusIn(eq(PROFILE_ID), anyCollection()))
                .thenReturn(false);

        LocalTime now = LocalTime.now();
        LocalTime pastTime = now.minusMinutes(30);
        if (!pastTime.isBefore(now)) {
            pastTime = LocalTime.of(0, 0);
        }
        final LocalTime past = pastTime;

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.createRequest(new NearbyServiceRequestRequest(
                        PROFILE_ID, ST_ID, LAT, LNG, null, past, null, null)));

        assertEquals("Preferred time must not be in the past", ex.getMessage());
    }

    @Test
    void createRequest_laterTodayTime_isAllowed() {
        when(profileService.getProfile(PROFILE_ID)).thenReturn(patientProfile());
        when(serviceTypeRepository.findById(ST_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceRequestRepository.existsByProfile_IdAndIsDeletedFalseAndStatusIn(eq(PROFILE_ID), anyCollection()))
                .thenReturn(false);
        stubTwoNearbyNurses();
        stubSaveAndFlush();

        NearbyServiceRequestResponse response = service.createRequest(new NearbyServiceRequestRequest(
                PROFILE_ID, ST_ID, LAT, LNG, null, LocalTime.now().plusMinutes(30), "desc", null));

        assertEquals(REQ_ID, response.serviceRequestId());
    }

    @Test
    void createRequest_noPreferredTime_isAllowed() {
        when(profileService.getProfile(PROFILE_ID)).thenReturn(patientProfile());
        when(serviceTypeRepository.findById(ST_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceRequestRepository.existsByProfile_IdAndIsDeletedFalseAndStatusIn(eq(PROFILE_ID), anyCollection()))
                .thenReturn(false);
        stubTwoNearbyNurses();
        stubSaveAndFlush();

        NearbyServiceRequestResponse response = service.createRequest(new NearbyServiceRequestRequest(
                PROFILE_ID, ST_ID, LAT, LNG, LocalDate.now().plusDays(1), null, "desc", null));

        assertEquals(REQ_ID, response.serviceRequestId());
    }

    @Test
    void createRequest_activeRequestExists_throws() {
        when(profileService.getProfile(PROFILE_ID)).thenReturn(patientProfile());
        when(serviceTypeRepository.findById(ST_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceRequestRepository.existsByProfile_IdAndIsDeletedFalseAndStatusIn(eq(PROFILE_ID), anyCollection()))
                .thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.createRequest(new NearbyServiceRequestRequest(
                        PROFILE_ID, ST_ID, LAT, LNG, LocalDate.now().plusDays(1), null, "desc", null)));

        assertEquals("This profile already has an active service request", ex.getMessage());
    }

    @Test
    void createRequest_nullDescription_buildsBrief() {
        when(profileService.getProfile(PROFILE_ID)).thenReturn(patientProfile());
        when(serviceTypeRepository.findById(ST_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceRequestRepository.existsByProfile_IdAndIsDeletedFalseAndStatusIn(eq(PROFILE_ID), anyCollection()))
                .thenReturn(false);
        stubTwoNearbyNurses();
        stubSaveAndFlush();

        service.createRequest(new NearbyServiceRequestRequest(
                PROFILE_ID, ST_ID, LAT, LNG, LocalDate.now().plusDays(1), null, null, null));

        ArgumentCaptor<ServiceRequest> captor = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(serviceRequestRepository).saveAndFlush(captor.capture());
        String description = captor.getValue().getServiceDescription();
        assertTrue(description.startsWith("Patient: "));
        assertTrue(description.contains("male"));
        assertTrue(description.contains("blood type A+"));
        assertTrue(description.contains("Conditions: Hypertension."));
        assertTrue(description.contains("Allergies: Penicillin."));
        assertTrue(description.contains("Medications: Aspirin."));
        assertTrue(description.contains("Requested service: Home Nursing."));
    }

    @Test
    void createRequest_blankDescription_buildsBrief() {
        when(profileService.getProfile(PROFILE_ID)).thenReturn(patientProfile());
        when(serviceTypeRepository.findById(ST_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceRequestRepository.existsByProfile_IdAndIsDeletedFalseAndStatusIn(eq(PROFILE_ID), anyCollection()))
                .thenReturn(false);
        stubTwoNearbyNurses();
        stubSaveAndFlush();

        service.createRequest(new NearbyServiceRequestRequest(
                PROFILE_ID, ST_ID, LAT, LNG, LocalDate.now().plusDays(1), null, "   ", null));

        ArgumentCaptor<ServiceRequest> captor = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(serviceRequestRepository).saveAndFlush(captor.capture());
        assertNotNull(captor.getValue().getServiceDescription());
        assertTrue(captor.getValue().getServiceDescription().contains("Requested service: Home Nursing."));
    }

    @Test
    void createRequest_noNearbyNurses_returnsEmptyNearbyList() {
        when(profileService.getProfile(PROFILE_ID)).thenReturn(patientProfile());
        when(serviceTypeRepository.findById(ST_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceRequestRepository.existsByProfile_IdAndIsDeletedFalseAndStatusIn(eq(PROFILE_ID), anyCollection()))
                .thenReturn(false);
        when(nurseServiceRepository.findByServiceType_IdAndIsActiveTrue(ST_ID)).thenReturn(List.of());
        when(webSocketPresenceService.getOnlineNurses()).thenReturn(new HashSet<>());
        stubSaveAndFlush();

        NearbyServiceRequestResponse response = service.createRequest(new NearbyServiceRequestRequest(
                PROFILE_ID, ST_ID, LAT, LNG, LocalDate.now().plusDays(1), null, "desc", null));

        assertTrue(response.nearbyNurses().isEmpty());
    }

    @Test
    void createRequest_serviceTypeNotFound_throws() {
        when(profileService.getProfile(PROFILE_ID)).thenReturn(patientProfile());
        when(serviceTypeRepository.findById(ST_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                service.createRequest(new NearbyServiceRequestRequest(
                        PROFILE_ID, ST_ID, LAT, LNG, null, null, null, null)));

        assertTrue(ex.getMessage().contains("Service type not found"));
    }

    // ------------------------------------------------------------------
    // listNearbyForNurse
    // ------------------------------------------------------------------

    @Test
    void listNearbyForNurse_nurseNotFound_throws() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listNearbyForNurse(NURSE_USER_ID));
    }

    @Test
    void listNearbyForNurse_notApproved_throws() {
        Nurse pendingNurse = Nurse.builder()
                .id(NURSE_ID).user(nurseUser())
                .verificationStatus(VerificationStatus.UNDER_REVIEW)
                .build();
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(pendingNurse));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.listNearbyForNurse(NURSE_USER_ID));

        assertEquals("Nurse is not eligible to receive service requests", ex.getMessage());
    }

    @Test
    void listNearbyForNurse_locationUnavailable_throws() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.listNearbyForNurse(NURSE_USER_ID));

        assertEquals("Nurse location is unavailable", ex.getMessage());
    }

    @Test
    void listNearbyForNurse_noActiveServiceTypes_returnsEmpty() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.0000, 30.0000)));
        when(nurseServiceRepository.findByNurse_IdAndIsActiveTrue(NURSE_ID)).thenReturn(List.of());

        List<NearbyNurseServiceRequestResponse> result = service.listNearbyForNurse(NURSE_USER_ID);

        assertTrue(result.isEmpty());
        verify(serviceRequestRepository, never()).findOpenRequestsForServiceTypes(anyList(), anyList());
    }

    @Test
    void listNearbyForNurse_happyPath_filtersAndSorts() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.0000, 30.0000)));
        when(nurseServiceRepository.findByNurse_IdAndIsActiveTrue(NURSE_ID))
                .thenReturn(List.of(nurseService(assignedNurse(), serviceType()),
                        nurseService(assignedNurse(), serviceType2())));

        ServiceRequest close = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .serviceDescription("desc").status(ServiceRequestStatus.PENDING)
                .latitude(new BigDecimal("30.0005")).longitude(new BigDecimal("31.0005")).createdAt(NOW).build();
        ServiceRequest far = ServiceRequest.builder()
                .id(REQ_ID_2).profile(patientProfile()).serviceType(serviceType())
                .serviceDescription("desc").status(ServiceRequestStatus.PENDING)
                .latitude(new BigDecimal("30.6000")).longitude(new BigDecimal("31.6000")).createdAt(NOW).build();
        ServiceRequest nullLat = ServiceRequest.builder()
                .id(UUID.randomUUID()).profile(patientProfile()).serviceType(serviceType())
                .serviceDescription("desc").status(ServiceRequestStatus.PENDING)
                .longitude(new BigDecimal("31.0005")).createdAt(NOW).build();
        ServiceRequest nullLng = ServiceRequest.builder()
                .id(UUID.randomUUID()).profile(patientProfile()).serviceType(serviceType())
                .serviceDescription("desc").status(ServiceRequestStatus.PENDING)
                .latitude(new BigDecimal("30.0005")).createdAt(NOW).build();
        when(serviceRequestRepository.findOpenRequestsForServiceTypes(anyList(), anyList()))
                .thenReturn(List.of(close, far, nullLat, nullLng));

        List<NearbyNurseServiceRequestResponse> result = service.listNearbyForNurse(NURSE_USER_ID);

        assertEquals(1, result.size());
        NearbyNurseServiceRequestResponse response = result.get(0);
        assertEquals(REQ_ID, response.serviceRequestId());
        assertEquals(PROFILE_ID, response.profileId());
        assertEquals("Mona", response.patientFirstName());
        assertEquals("Ali", response.patientLastName());
        assertEquals("patient-img", response.patientProfileImageUrl());
        assertEquals(ST_ID, response.serviceTypeId());
        assertEquals("Home Nursing", response.serviceName());
        assertEquals(ServiceRequestStatus.PENDING, response.status());
        assertTrue(response.distanceKm() > 0 && response.distanceKm() < 0.2);
        assertEquals(new BigDecimal("500.00"), response.estimatedPrice());
        assertEquals(120, response.estimatedDurationMinutes());
        assertEquals(NOW, response.createdAt());
    }

    // ------------------------------------------------------------------
    // cancelRequest
    // ------------------------------------------------------------------

    @Test
    void cancelRequest_notFound_throws() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.cancelRequest(REQ_ID, PATIENT_USER_ID));
    }

    @Test
    void cancelRequest_unrelatedUser_throws() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(request()));

        assertThrows(ResourceNotFoundException.class, () -> service.cancelRequest(REQ_ID, OTHER_USER_ID));
    }

    @Test
    void cancelRequest_profileNull_throws() {
        ServiceRequest noProfile = ServiceRequest.builder()
                .id(REQ_ID).profile(null)
                .serviceType(serviceType()).status(ServiceRequestStatus.SEARCHING)
                .latitude(LAT).longitude(LNG).isDeleted(false).build();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(noProfile));

        assertThrows(ResourceNotFoundException.class, () -> service.cancelRequest(REQ_ID, PATIENT_USER_ID));
    }

    @Test
    void cancelRequest_ownerProfileUserNull_throws() {
        ServiceRequest noUser = ServiceRequest.builder()
                .id(REQ_ID).profile(Profile.builder().id(PROFILE_ID).build())
                .serviceType(serviceType()).status(ServiceRequestStatus.SEARCHING)
                .latitude(LAT).longitude(LNG).isDeleted(false).build();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(noUser));

        assertThrows(ResourceNotFoundException.class, () -> service.cancelRequest(REQ_ID, PATIENT_USER_ID));
    }

    @Test
    void cancelRequest_ownerWithAssignedNurse_notifiesNurseAndRejectsPendingOffers() {
        ServiceRequest sr = requestWithNurse();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(sr));
        NurseOffer pending = offer(OFFER_ID, new SharedOffer(assignedNurse()), NurseOfferStatus.PENDING);
        NurseOffer accepted = offer(UUID.randomUUID(), new SharedOffer(assignedNurse()), NurseOfferStatus.ACCEPTED);
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQ_ID))
                .thenReturn(List.of(pending, accepted));
        runAfterCommitEvents();

        service.cancelRequest(REQ_ID, PATIENT_USER_ID);

        assertEquals(ServiceRequestStatus.CANCELLED, sr.getStatus());
        ArgumentCaptor<NotificationRequest> notifyCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(1)).create(notifyCaptor.capture());
        assertEquals(NURSE_USER_ID, notifyCaptor.getValue().userId());
        assertEquals(NotificationType.BOOKING, notifyCaptor.getValue().type());
        assertEquals("SERVICE_REQUEST", notifyCaptor.getValue().relatedEntityType());
        assertEquals(REQ_ID, notifyCaptor.getValue().relatedEntityId());
        assertEquals(NurseOfferStatus.REJECTED, pending.getStatus());
        assertEquals(NurseOfferStatus.ACCEPTED, accepted.getStatus());
        verify(tokenService).delete(visitCodeKey());
        verify(tokenService).delete(visitCodeAttemptsKey());
        ArgumentCaptor<ReservationEvent> eventCaptor = ArgumentCaptor.forClass(ReservationEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/reservation/" + REQ_ID), eventCaptor.capture());
        assertEquals("REQUEST_CANCELLED", eventCaptor.getValue().type());
        assertEquals(REQ_ID, eventCaptor.getValue().reservationId());
        assertEquals(Map.of(), eventCaptor.getValue().data());
    }

    @Test
    void cancelRequest_ownerNurseUserNull_skipsNurseNotification() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .nurse(Nurse.builder().id(NURSE_ID).build())
                .status(ServiceRequestStatus.SEARCHING).latitude(LAT).longitude(LNG)
                .isDeleted(false).build();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(sr));

        service.cancelRequest(REQ_ID, PATIENT_USER_ID);

        verifyNoInteractions(notificationService);
        verify(tokenService).delete(visitCodeKey());
        verify(tokenService).delete(visitCodeAttemptsKey());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ReservationEvent.class));
    }

    @Test
    void cancelRequest_ownerUnassigned_notifiesDistinctPendingOfferNurses() {
        ServiceRequest sr = request();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(sr));
        NurseOffer o1 = offer(UUID.randomUUID(), new SharedOffer(nurseWithUserId(NURSE_ID, NURSE_USER_ID)), NurseOfferStatus.PENDING);
        NurseOffer o2 = offer(UUID.randomUUID(), new SharedOffer(nurseWithUserId(NURSE_ID, NURSE_USER_ID)), NurseOfferStatus.PENDING);
        NurseOffer o3 = offer(UUID.randomUUID(), new SharedOffer(nurseWithUserId(NURSE_ID_2, NURSE_USER_ID_2)), NurseOfferStatus.PENDING);
        NurseOffer o4 = offer(UUID.randomUUID(), new SharedOffer(nurseWithUserId(UUID.randomUUID(), OTHER_USER_ID)), NurseOfferStatus.ACCEPTED);
        NurseOffer o5 = offer(UUID.randomUUID(), new SharedOffer(Nurse.builder().id(UUID.randomUUID()).build()), NurseOfferStatus.PENDING);
        NurseOffer o6 = offer(UUID.randomUUID(), null, NurseOfferStatus.PENDING);
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQ_ID))
                .thenReturn(List.of(o1, o2, o3, o4, o5, o6));
        runAfterCommitEvents();

        service.cancelRequest(REQ_ID, PATIENT_USER_ID);

        ArgumentCaptor<NotificationRequest> notifyCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(2)).create(notifyCaptor.capture());
        assertEquals(Set.of(NURSE_USER_ID, NURSE_USER_ID_2),
                notifyCaptor.getAllValues().stream().map(NotificationRequest::userId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(NurseOfferStatus.REJECTED, o1.getStatus());
        assertEquals(NurseOfferStatus.REJECTED, o2.getStatus());
        assertEquals(NurseOfferStatus.REJECTED, o3.getStatus());
        assertEquals(NurseOfferStatus.ACCEPTED, o4.getStatus());
        assertEquals(NurseOfferStatus.REJECTED, o5.getStatus());
        assertEquals(NurseOfferStatus.REJECTED, o6.getStatus());
    }

    @Test
    void cancelRequest_assignedNurseCancels_notifiesPatient() {
        ServiceRequest sr = requestWithNurse();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(sr));
        runAfterCommitEvents();

        service.cancelRequest(REQ_ID, NURSE_USER_ID);

        assertEquals(ServiceRequestStatus.CANCELLED, sr.getStatus());
        ArgumentCaptor<NotificationRequest> notifyCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(1)).create(notifyCaptor.capture());
        assertEquals(PATIENT_USER_ID, notifyCaptor.getValue().userId());
    }

    @Test
    void cancelRequest_assignedNurseProfileNull_skipsPatientNotify() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID).profile(null)
                .serviceType(serviceType())
                .nurse(assignedNurse())
                .status(ServiceRequestStatus.PENDING).latitude(LAT).longitude(LNG)
                .isDeleted(false).build();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(sr));

        service.cancelRequest(REQ_ID, NURSE_USER_ID);

        verifyNoInteractions(notificationService);
        assertEquals(ServiceRequestStatus.CANCELLED, sr.getStatus());
    }

    @Test
    void cancelRequest_assignedNurseProfileUserNull_skipsPatientNotify() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID)
                .profile(Profile.builder().id(PROFILE_ID).firstName("Mona").build())
                .serviceType(serviceType())
                .nurse(assignedNurse())
                .status(ServiceRequestStatus.PENDING).latitude(LAT).longitude(LNG)
                .isDeleted(false).build();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(sr));

        service.cancelRequest(REQ_ID, NURSE_USER_ID);

        verifyNoInteractions(notificationService);
        assertEquals(ServiceRequestStatus.CANCELLED, sr.getStatus());
    }

    @Test
    void cancelRequest_nonCancellableStatus_throws() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .status(ServiceRequestStatus.IN_PROGRESS).latitude(LAT).longitude(LNG)
                .isDeleted(false).build();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(sr));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.cancelRequest(REQ_ID, PATIENT_USER_ID));

        assertEquals("This service request cannot be cancelled", ex.getMessage());
        verifyNoInteractions(tokenService, messagingTemplate, notificationService);
    }

    // ------------------------------------------------------------------
    // getNearbyNursesForRequest
    // ------------------------------------------------------------------

    @Test
    void getNearbyNursesForRequest_notFound_throws() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getNearbyNursesForRequest(REQ_ID, PATIENT_USER_ID));
    }

    @Test
    void getNearbyNursesForRequest_notOwner_throws() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(request()));

        assertThrows(ResourceNotFoundException.class, () ->
                service.getNearbyNursesForRequest(REQ_ID, OTHER_USER_ID));
    }

    @Test
    void getNearbyNursesForRequest_notOpenStatus_throws() {
        ServiceRequest sr = requestWithNurse();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(sr));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.getNearbyNursesForRequest(REQ_ID, PATIENT_USER_ID));

        assertEquals("This service request is not open for matching anymore", ex.getMessage());
    }

    @Test
    void getNearbyNursesForRequest_happy_returnsNearbySorted() {
        ServiceRequest sr = request();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(sr));
        stubTwoNearbyNurses();

        List<NearbyNurse> result = service.getNearbyNursesForRequest(REQ_ID, PATIENT_USER_ID);

        assertEquals(2, result.size());
        assertEquals(NURSE_ID, result.get(0).nurseId());
        assertEquals(0.0, result.get(0).distanceKm());
        assertTrue(result.get(1).distanceKm() > 0);
        assertEquals(0, result.get(1).latitude().compareTo(new BigDecimal("30.0050")));
        assertEquals(0, result.get(1).longitude().compareTo(new BigDecimal("31.0100")));
    }

    // ------------------------------------------------------------------
    // cancelOpenRequestsForUser
    // ------------------------------------------------------------------

    @Test
    void cancelOpenRequestsForUser_noOpenRequests_doesNothing() {
        when(serviceRequestRepository
                .findByProfile_User_IdAndIsDeletedFalseAndStatusInAndNurseNullOrderByCreatedAtDesc(
                        eq(PATIENT_USER_ID), anyCollection()))
                .thenReturn(List.of());

        service.cancelOpenRequestsForUser(PATIENT_USER_ID);

        verifyNoInteractions(messagingTemplate, notificationService, tokenService);
    }

    @Test
    void cancelOpenRequestsForUser_cancelsEachOpenRequest() {
        ServiceRequest r1 = request();
        ServiceRequest r2 = ServiceRequest.builder()
                .id(REQ_ID_2).profile(patientProfile()).serviceType(serviceType())
                .status(ServiceRequestStatus.BOOKING).latitude(LAT).longitude(LNG)
                .isDeleted(false).createdAt(NOW_2).build();
        when(serviceRequestRepository
                .findByProfile_User_IdAndIsDeletedFalseAndStatusInAndNurseNullOrderByCreatedAtDesc(
                        eq(PATIENT_USER_ID), anyCollection()))
                .thenReturn(List.of(r1, r2));
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(any(UUID.class)))
                .thenReturn(Optional.of(r1), Optional.of(r2));
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(any(UUID.class)))
                .thenReturn(List.of());
        runAfterCommitEvents();

        service.cancelOpenRequestsForUser(PATIENT_USER_ID);

        assertEquals(ServiceRequestStatus.CANCELLED, r1.getStatus());
        assertEquals(ServiceRequestStatus.CANCELLED, r2.getStatus());
        ArgumentCaptor<ReservationEvent> eventCaptor = ArgumentCaptor.forClass(ReservationEvent.class);
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), eventCaptor.capture());
        assertEquals(2, eventCaptor.getAllValues().size());
        assertTrue(eventCaptor.getAllValues().stream()
                .allMatch(event -> "REQUEST_CANCELLED".equals(event.type())));
    }

    // ------------------------------------------------------------------
    // generateVisitCode
    // ------------------------------------------------------------------

    @Test
    void generateVisitCode_notFound_throws() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.generateVisitCode(REQ_ID, PATIENT_USER_ID));
    }

    @Test
    void generateVisitCode_notOwner_throws() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(request()));

        assertThrows(ResourceNotFoundException.class, () -> service.generateVisitCode(REQ_ID, OTHER_USER_ID));
    }

    @Test
    void generateVisitCode_notAcceptedStatus_throws() {
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(request()));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.generateVisitCode(REQ_ID, PATIENT_USER_ID));

        assertEquals("Visit code can only be generated for an accepted reservation", ex.getMessage());
    }

    @Test
    void generateVisitCode_existingCode_returnsStoredCode() {
        ServiceRequest sr = requestWithNurse();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(sr));
        when(tokenService.get(visitCodeKey())).thenReturn("AB12CD34");

        VisitCodeResponse response = service.generateVisitCode(REQ_ID, PATIENT_USER_ID);

        assertEquals(REQ_ID, response.serviceRequestId());
        assertEquals("AB12CD34", response.code());
        assertTrue(response.expiresAt().isAfter(Instant.now()));
        verify(tokenService, never()).set(anyString(), anyString(), any());
    }

    @Test
    void generateVisitCode_newCode_storesNotifiesAndReturns() {
        ServiceRequest sr = requestWithNurse();
        when(serviceRequestRepository.findByIdAndIsDeletedFalse(REQ_ID)).thenReturn(Optional.of(sr));
        when(tokenService.get(visitCodeKey())).thenReturn(null);

        VisitCodeResponse response = service.generateVisitCode(REQ_ID, PATIENT_USER_ID);

        assertEquals(REQ_ID, response.serviceRequestId());
        assertTrue(response.code().matches("[0-9A-Z]{8}"));
        assertTrue(response.expiresAt().isAfter(Instant.now()));
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(tokenService).set(eq(visitCodeKey()), codeCaptor.capture(), eq(Duration.ofHours(24)));
        assertEquals(response.code(), codeCaptor.getValue());
        verify(tokenService).delete(visitCodeAttemptsKey());
        ArgumentCaptor<NotificationRequest> notifyCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).create(notifyCaptor.capture());
        assertEquals(PATIENT_USER_ID, notifyCaptor.getValue().userId());
        assertEquals("Visit Code Ready", notifyCaptor.getValue().title());
        assertEquals(NotificationType.BOOKING, notifyCaptor.getValue().type());
    }

    // ------------------------------------------------------------------
    // completeRequest
    // ------------------------------------------------------------------

    private void stubNurseAndRequest(ServiceRequest sr) {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));
    }

    @Test
    void completeRequest_nurseNotFound_throws() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.completeRequest(REQ_ID, "ABCD1234", NURSE_USER_ID));
    }

    @Test
    void completeRequest_requestNotFound_throws() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.completeRequest(REQ_ID, "ABCD1234", NURSE_USER_ID));
    }

    @Test
    void completeRequest_noAssignedNurse_throwsForbidden() {
        ServiceRequest sr = request();
        stubNurseAndRequest(sr);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                service.completeRequest(REQ_ID, "ABCD1234", NURSE_USER_ID));

        assertEquals("Only the assigned nurse can complete this reservation", ex.getMessage());
    }

    @Test
    void completeRequest_differentAssignedNurse_throwsForbidden() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .nurse(nurseWithUserId(NURSE_ID_2, NURSE_USER_ID_2))
                .status(ServiceRequestStatus.ACCEPTED).latitude(LAT).longitude(LNG)
                .isDeleted(false).build();
        stubNurseAndRequest(sr);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                service.completeRequest(REQ_ID, "ABCD1234", NURSE_USER_ID));

        assertEquals("Only the assigned nurse can complete this reservation", ex.getMessage());
    }

    @Test
    void completeRequest_notAcceptedStatus_throws() {
        ServiceRequest sr = requestWithNurse();
        sr.setStatus(ServiceRequestStatus.IN_PROGRESS);
        stubNurseAndRequest(sr);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.completeRequest(REQ_ID, "ABCD1234", NURSE_USER_ID));

        assertEquals("This service request cannot be completed", ex.getMessage());
    }

    @Test
    void completeRequest_noStoredCode_throws() {
        ServiceRequest sr = requestWithNurse();
        stubNurseAndRequest(sr);
        when(tokenService.get(visitCodeKey())).thenReturn(null);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.completeRequest(REQ_ID, "ABCD1234", NURSE_USER_ID));

        assertEquals("Visit code is expired or was never generated", ex.getMessage());
    }

    @Test
    void completeRequest_wrongCode_firstAttempt_renewsAttemptsTtl() {
        ServiceRequest sr = requestWithNurse();
        stubNurseAndRequest(sr);
        when(tokenService.get(visitCodeKey())).thenReturn("SECRET1");
        when(tokenService.increment(visitCodeAttemptsKey())).thenReturn(1L);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.completeRequest(REQ_ID, "WRONG", NURSE_USER_ID));

        assertEquals("Invalid visit code", ex.getMessage());
        verify(tokenService).expire(visitCodeAttemptsKey(), Duration.ofHours(24));
        verify(tokenService, never()).delete(visitCodeKey());
        verify(tokenService, never()).delete(visitCodeAttemptsKey());
    }

    @Test
    void completeRequest_wrongCode_midAttempts_noTtlNoDelete() {
        ServiceRequest sr = requestWithNurse();
        stubNurseAndRequest(sr);
        when(tokenService.get(visitCodeKey())).thenReturn("SECRET1");
        when(tokenService.increment(visitCodeAttemptsKey())).thenReturn(3L);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.completeRequest(REQ_ID, "WRONG", NURSE_USER_ID));

        assertEquals("Invalid visit code", ex.getMessage());
        verify(tokenService, never()).expire(anyString(), any());
        verify(tokenService, never()).delete(anyString());
    }

    @Test
    void completeRequest_wrongCode_maxAttempts_invalidatesCode() {
        ServiceRequest sr = requestWithNurse();
        stubNurseAndRequest(sr);
        when(tokenService.get(visitCodeKey())).thenReturn("SECRET1");
        when(tokenService.increment(visitCodeAttemptsKey())).thenReturn(5L);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.completeRequest(REQ_ID, "WRONG", NURSE_USER_ID));

        assertEquals("Too many failed attempts. Visit code has been invalidated.", ex.getMessage());
        verify(tokenService).delete(visitCodeKey());
        verify(tokenService).delete(visitCodeAttemptsKey());
    }

    @Test
    void completeRequest_nullVisitCodeAndNullAttempts_throwsInvalid() {
        ServiceRequest sr = requestWithNurse();
        stubNurseAndRequest(sr);
        when(tokenService.get(visitCodeKey())).thenReturn("SECRET1");
        when(tokenService.increment(visitCodeAttemptsKey())).thenReturn(null);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.completeRequest(REQ_ID, null, NURSE_USER_ID));

        assertEquals("Invalid visit code", ex.getMessage());
        verify(tokenService, never()).expire(anyString(), any());
        verify(tokenService, never()).delete(anyString());
    }

    @Test
    void completeRequest_happyPath_completesAndNotifies() {
        ServiceRequest sr = requestWithNurse();
        stubNurseAndRequest(sr);
        when(tokenService.get(visitCodeKey())).thenReturn("ABCD1234");
        runAfterCommitEvents();

        service.completeRequest(REQ_ID, "ABCD1234", NURSE_USER_ID);

        assertEquals(ServiceRequestStatus.COMPLETED, sr.getStatus());
        verify(serviceRequestRepository).save(sr);
        verify(tokenService).delete(visitCodeKey());
        verify(tokenService).delete(visitCodeAttemptsKey());
        ArgumentCaptor<ReservationEvent> eventCaptor = ArgumentCaptor.forClass(ReservationEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/reservation/" + REQ_ID), eventCaptor.capture());
        assertEquals("COMPLETED", eventCaptor.getValue().type());
        assertEquals(REQ_ID, eventCaptor.getValue().reservationId());
        assertNull(eventCaptor.getValue().data());
        ArgumentCaptor<NotificationRequest> notifyCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).create(notifyCaptor.capture());
        assertEquals(PATIENT_USER_ID, notifyCaptor.getValue().userId());
        assertEquals("Reservation Completed", notifyCaptor.getValue().title());
    }

    // ------------------------------------------------------------------
    // getDetails / toDetails
    // ------------------------------------------------------------------

    @Test
    void getDetails_notFound_throws() {
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getDetails(REQ_ID, PATIENT_USER_ID));
    }

    @Test
    void getDetails_deleted_throws() {
        ServiceRequest sr = requestWithNurse();
        sr.setIsDeleted(true);
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));

        assertThrows(ResourceNotFoundException.class, () -> service.getDetails(REQ_ID, PATIENT_USER_ID));
    }

    @Test
    void getDetails_notParticipant_throws() {
        ServiceRequest sr = request();
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));
        when(serviceRequestRepository.isParticipant(REQ_ID, PATIENT_USER_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.getDetails(REQ_ID, PATIENT_USER_ID));
    }

    @Test
    void getDetails_happy_returnsFullDetailsWithOffers() {
        ServiceRequest sr = requestWithNurse();
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));
        when(serviceRequestRepository.isParticipant(REQ_ID, PATIENT_USER_ID)).thenReturn(true);
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.0010, 30.0010)));

        NurseOffer offer = offer(OFFER_ID, new SharedOffer(assignedNurse()), NurseOfferStatus.PENDING);
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQ_ID))
                .thenReturn(List.of(offer));
        NurseSummaryResponse summary = new NurseSummaryResponse(
                NURSE_ID, "Sara", "Hassan", "nurse-img",
                new BigDecimal("4.8"), 10);
        when(nurseOfferMapper.toResponse(offer)).thenReturn(new NurseOfferResponse(
                OFFER_ID, REQ_ID, summary, new BigDecimal("350"),
                LocalDate.now().plusDays(2), LocalTime.of(10, 0), "I can help",
                NurseOfferStatus.PENDING, 999.0, "stub-name", 999, NOW, NOW_2));

        ServiceRequestDetailsResponse response = service.getDetails(REQ_ID, PATIENT_USER_ID);

        assertEquals(REQ_ID, response.serviceRequestId());
        assertEquals(ST_ID, response.serviceType().id());
        assertEquals("Home Nursing", response.serviceType().name());
        assertEquals(BASE_PRICE, response.serviceType().basePrice());
        assertEquals(120, response.serviceType().estimatedDurationMinutes());
        assertEquals(PROFILE_ID, response.profile().id());
        assertEquals("Mona", response.profile().firstName());
        assertEquals("01012345678", response.profile().phoneNumber());
        assertEquals("patient-img", response.profile().profileImageUrl());
        assertEquals(NURSE_ID, response.nurse().id());
        assertEquals("Sara", response.nurse().firstName());
        assertEquals(new BigDecimal("4.8"), response.nurse().ratingAvg());
        assertEquals(10, response.nurse().totalReviews());
        assertEquals("In-home nursing care", response.serviceDescription());
        assertEquals(90, response.durationMinutes());
        assertEquals(ServiceRequestStatus.ACCEPTED, response.status());
        assertTrue(response.distanceKm() > 0 && response.distanceKm() < 0.2);
        assertEquals(NOW, response.createdAt());
        assertEquals(NOW_2, response.updatedAt());
        assertEquals(1, response.offers().size());
        assertEquals(OFFER_ID, response.offers().get(0).id());
        assertEquals(REQ_ID, response.offers().get(0).serviceRequestId());
        assertEquals(summary, response.offers().get(0).nurse());
        assertEquals("Home Nursing", response.offers().get(0).serviceTypeName());
        assertEquals(120, response.offers().get(0).estimatedDurationMinutes());
        assertTrue(response.offers().get(0).distanceKm() > 0 && response.offers().get(0).distanceKm() < 0.2);
        assertEquals("I can help", response.offers().get(0).message());
        assertEquals(new BigDecimal("350"), response.offers().get(0).proposedPrice());
    }

    @Test
    void getDetails_noNurseAndNoServiceType_returnsNullSummaries() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile())
                .serviceType(null).nurse(null)
                .serviceDescription("desc").status(ServiceRequestStatus.PENDING)
                .latitude(LAT).longitude(LNG)
                .isDeleted(false).createdAt(NOW).updatedAt(NOW_2).build();
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));
        when(serviceRequestRepository.isParticipant(REQ_ID, PATIENT_USER_ID)).thenReturn(true);
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQ_ID))
                .thenReturn(List.of());

        ServiceRequestDetailsResponse response = service.getDetails(REQ_ID, PATIENT_USER_ID);

        assertNull(response.serviceType());
        assertNull(response.nurse());
        assertNull(response.distanceKm());
        assertTrue(response.offers().isEmpty());
    }

    @Test
    void getDetails_offerWithoutServiceType_returnsNullServiceFields() {
        ServiceRequest sr = request();
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));
        when(serviceRequestRepository.isParticipant(REQ_ID, PATIENT_USER_ID)).thenReturn(true);
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.0010, 30.0010)));

        ServiceRequest noType = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(null)
                .nurse(assignedNurse())
                .status(ServiceRequestStatus.PENDING).latitude(LAT).longitude(LNG).isDeleted(false).build();
        NurseOffer offer = NurseOffer.builder()
                .id(OFFER_ID).serviceRequest(noType).nurse(null)
                .proposedPrice(new BigDecimal("300")).status(NurseOfferStatus.PENDING)
                .isDeleted(false).createdAt(NOW).updatedAt(NOW_2).build();
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQ_ID))
                .thenReturn(List.of(offer));
        when(nurseOfferMapper.toResponse(offer)).thenReturn(new NurseOfferResponse(
                OFFER_ID, REQ_ID, new NurseSummaryResponse(NURSE_ID, "Sara", "Hassan", "nurse-img",
                        new BigDecimal("4.8"), 10),
                new BigDecimal("300"), null, null, null, NurseOfferStatus.PENDING,
                999.0, "stub", 999, NOW, NOW_2));

        ServiceRequestDetailsResponse response = service.getDetails(REQ_ID, PATIENT_USER_ID);

        assertEquals(1, response.offers().size());
        assertNull(response.offers().get(0).serviceTypeName());
        assertNull(response.offers().get(0).estimatedDurationMinutes());
        assertNull(response.offers().get(0).distanceKm());
    }

    // ------------------------------------------------------------------
    // getCurrentVisit
    // ------------------------------------------------------------------

    @Test
    void getCurrentVisit_asNurse_returnsNurseVisit() {
        ServiceRequest sr = requestWithNurse();
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findFirstByNurse_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                NURSE_ID, Set.of(ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.IN_PROGRESS)))
                .thenReturn(Optional.of(sr));
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQ_ID))
                .thenReturn(List.of());
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.0000, 30.0000)));

        ServiceRequestDetailsResponse response = service.getCurrentVisit(NURSE_USER_ID);

        assertEquals(REQ_ID, response.serviceRequestId());
        assertEquals(ServiceRequestStatus.ACCEPTED, response.status());
        assertEquals(NURSE_ID, response.nurse().id());
    }

    @Test
    void getCurrentVisit_noNurse_fallsBackToPatientProfile() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .status(ServiceRequestStatus.IN_PROGRESS).latitude(LAT).longitude(LNG)
                .isDeleted(false).createdAt(NOW).updatedAt(NOW_2).build();
        when(nurseRepository.findByUser_Id(PATIENT_USER_ID)).thenReturn(Optional.empty());
        when(serviceRequestRepository.findFirstByNurse_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                any(UUID.class), anyCollection())).thenReturn(Optional.empty());
        when(serviceRequestRepository.findFirstByProfile_User_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                PATIENT_USER_ID, Set.of(ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.IN_PROGRESS)))
                .thenReturn(Optional.of(sr));
        when(nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(REQ_ID))
                .thenReturn(List.of());

        ServiceRequestDetailsResponse response = service.getCurrentVisit(PATIENT_USER_ID);

        assertEquals(REQ_ID, response.serviceRequestId());
        assertNull(response.nurse());
        assertEquals(PROFILE_ID, response.profile().id());
    }

    @Test
    void getCurrentVisit_none_throws() {
        when(nurseRepository.findByUser_Id(PATIENT_USER_ID)).thenReturn(Optional.empty());
        when(serviceRequestRepository.findFirstByNurse_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                any(UUID.class), anyCollection())).thenReturn(Optional.empty());
        when(serviceRequestRepository.findFirstByProfile_User_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                eq(PATIENT_USER_ID), anyCollection())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getCurrentVisit(PATIENT_USER_ID));
    }

    // ------------------------------------------------------------------
    // getNursePreview
    // ------------------------------------------------------------------

    @Test
    void getNursePreview_nurseNotFound_throws() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getNursePreview(REQ_ID, NURSE_USER_ID));
    }

    @Test
    void getNursePreview_notApproved_throws() {
        Nurse pending = Nurse.builder().id(NURSE_ID).user(nurseUser())
                .verificationStatus(VerificationStatus.UNDER_REVIEW).build();
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(pending));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.getNursePreview(REQ_ID, NURSE_USER_ID));

        assertEquals("Nurse is not eligible to receive service requests", ex.getMessage());
    }

    @Test
    void getNursePreview_requestNotFound_throws() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getNursePreview(REQ_ID, NURSE_USER_ID));
    }

    @Test
    void getNursePreview_deletedRequest_throws() {
        ServiceRequest sr = request();
        sr.setIsDeleted(true);
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));

        assertThrows(ResourceNotFoundException.class, () -> service.getNursePreview(REQ_ID, NURSE_USER_ID));
    }

    @Test
    void getNursePreview_assignedNurse_throws() {
        ServiceRequest sr = requestWithNurse();
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));

        assertThrows(ResourceNotFoundException.class, () -> service.getNursePreview(REQ_ID, NURSE_USER_ID));
    }

    @Test
    void getNursePreview_closedStatus_throws() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .status(ServiceRequestStatus.ACCEPTED).latitude(LAT).longitude(LNG)
                .isDeleted(false).build();
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));

        assertThrows(ResourceNotFoundException.class, () -> service.getNursePreview(REQ_ID, NURSE_USER_ID));
    }

    @Test
    void getNursePreview_nullLatitude_throws() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .status(ServiceRequestStatus.PENDING).longitude(LNG).isDeleted(false).build();
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.getNursePreview(REQ_ID, NURSE_USER_ID));

        assertEquals("Service request location is unavailable", ex.getMessage());
    }

    @Test
    void getNursePreview_nullLongitude_throws() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .status(ServiceRequestStatus.PENDING).latitude(LAT).isDeleted(false).build();
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.getNursePreview(REQ_ID, NURSE_USER_ID));

        assertEquals("Service request location is unavailable", ex.getMessage());
    }

    @Test
    void getNursePreview_notProvidesService_throws() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(request()));
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, ST_ID)).thenReturn(Optional.empty());

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                service.getNursePreview(REQ_ID, NURSE_USER_ID));

        assertEquals("Nurse does not provide the requested service", ex.getMessage());
    }

    @Test
    void getNursePreview_serviceInactive_throws() {
        NurseService inactive = NurseService.builder()
                .id(UUID.randomUUID()).nurse(assignedNurse()).serviceType(serviceType())
                .isActive(false).build();
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(request()));
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, ST_ID))
                .thenReturn(Optional.of(inactive));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                service.getNursePreview(REQ_ID, NURSE_USER_ID));

        assertEquals("Nurse does not provide the requested service", ex.getMessage());
    }

    @Test
    void getNursePreview_nurseLocationUnavailable_throws() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(request()));
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, ST_ID))
                .thenReturn(Optional.of(nurseService(assignedNurse(), serviceType())));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.getNursePreview(REQ_ID, NURSE_USER_ID));

        assertEquals("Nurse location is unavailable", ex.getMessage());
    }

    @Test
    void getNursePreview_outsideRadius_throws() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(request()));
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, ST_ID))
                .thenReturn(Optional.of(nurseService(assignedNurse(), serviceType())));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.6000, 30.6000)));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                service.getNursePreview(REQ_ID, NURSE_USER_ID));

        assertEquals("Nurse is outside the matching radius for this service request", ex.getMessage());
    }

    @Test
    void getNursePreview_happy_returnsPreview() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(request()));
        when(nurseServiceRepository.findByNurse_IdAndServiceType_Id(NURSE_ID, ST_ID))
                .thenReturn(Optional.of(nurseService(assignedNurse(), serviceType())));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.0000, 30.0000)));

        ServiceRequestNursePreviewResponse response = service.getNursePreview(REQ_ID, NURSE_USER_ID);

        assertEquals(REQ_ID, response.serviceRequestId());
        assertEquals(ST_ID, response.serviceTypeId());
        assertEquals("Home Nursing", response.serviceName());
        assertEquals("In-home nursing care", response.serviceDescription());
        assertEquals(ServiceRequestStatus.SEARCHING, response.status());
        assertEquals(new BigDecimal("500.00"), response.estimatedPrice());
        assertEquals(NOW, response.createdAt());
        assertEquals(SUMMARY, response.patient());
    }

    // ------------------------------------------------------------------
    // getAssignedNurseProfile
    // ------------------------------------------------------------------

    @Test
    void getAssignedNurseProfile_notFound_throws() {
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getAssignedNurseProfile(REQ_ID, NURSE_USER_ID));
    }

    @Test
    void getAssignedNurseProfile_deleted_throws() {
        ServiceRequest sr = requestWithNurse();
        sr.setIsDeleted(true);
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));

        assertThrows(ResourceNotFoundException.class, () -> service.getAssignedNurseProfile(REQ_ID, NURSE_USER_ID));
    }

    @Test
    void getAssignedNurseProfile_noNurse_throws() {
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(request()));

        assertThrows(ResourceNotFoundException.class, () -> service.getAssignedNurseProfile(REQ_ID, NURSE_USER_ID));
    }

    @Test
    void getAssignedNurseProfile_notYourNurse_throws() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .nurse(nurseWithUserId(NURSE_ID, NURSE_USER_ID_2))
                .status(ServiceRequestStatus.ACCEPTED).latitude(LAT).longitude(LNG)
                .isDeleted(false).build();
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));

        assertThrows(ResourceNotFoundException.class, () -> service.getAssignedNurseProfile(REQ_ID, NURSE_USER_ID));
    }

    @Test
    void getAssignedNurseProfile_notAssignedYet_throws() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .nurse(assignedNurse())
                .status(ServiceRequestStatus.SEARCHING).latitude(LAT).longitude(LNG)
                .isDeleted(false).build();
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.getAssignedNurseProfile(REQ_ID, NURSE_USER_ID));

        assertEquals("Service request is not assigned yet", ex.getMessage());
    }

    @Test
    void getAssignedNurseProfile_happy_returnsProfileWithAddress() {
        ServiceRequest sr = requestWithNurse();
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));
        when(addressRepository.findByProfileId(PROFILE_ID)).thenReturn(Optional.of(address()));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.0000, 30.0000)));

        ServiceRequestNurseProfileResponse response = service.getAssignedNurseProfile(REQ_ID, NURSE_USER_ID);

        assertEquals(REQ_ID, response.serviceRequestId());
        assertEquals(ST_ID, response.serviceTypeId());
        assertEquals("Home Nursing", response.serviceName());
        assertEquals(new BigDecimal("500.00"), response.estimatedPrice());
        assertEquals(SUMMARY, response.patient());
        assertEquals("01012345678", response.patientPhoneNumber());
        assertEquals("Egypt", response.address().country());
        assertEquals("Cairo", response.address().city());
        assertEquals("Zamalek", response.address().area());
        assertEquals("Tahrir", response.address().street());
        assertEquals("12", response.address().buildingNumber());
        assertEquals("3B", response.address().apartmentNumber());
        verify(patientMedicalSummaryAssembler).build(sr.getProfile(), true);
    }

    @Test
    void getAssignedNurseProfile_noAddressNoLocation_phoneNull() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(REQ_ID)
                .profile(Profile.builder().id(PROFILE_ID).firstName("Mona").build())
                .serviceType(serviceType())
                .nurse(assignedNurse())
                .status(ServiceRequestStatus.ACCEPTED)
                .latitude(LAT).longitude(LNG).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();
        when(serviceRequestRepository.findWithDetailsById(REQ_ID)).thenReturn(Optional.of(sr));
        when(addressRepository.findByProfileId(PROFILE_ID)).thenReturn(Optional.empty());
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR)).thenReturn(Optional.empty());
        when(patientMedicalSummaryAssembler.build(sr.getProfile(), true)).thenReturn(SUMMARY);

        ServiceRequestNurseProfileResponse response = service.getAssignedNurseProfile(REQ_ID, NURSE_USER_ID);

        assertNull(response.address());
        assertNull(response.patientPhoneNumber());
        assertEquals(new BigDecimal("500.00"), response.estimatedPrice());
    }

    // ------------------------------------------------------------------
    // listConfirmedHistory / toHistoryResponse
    // ------------------------------------------------------------------

    @Test
    void listConfirmedHistory_empty_returnsEmpty() {
        when(serviceRequestRepository.findByProfile_User_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                PATIENT_USER_ID, List.of(ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.IN_PROGRESS,
                        ServiceRequestStatus.COMPLETED)))
                .thenReturn(List.of());

        List<ServiceRequestHistoryResponse> result = service.listConfirmedHistory(PATIENT_USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void listConfirmedHistory_happy_returnsFullHistory() {
        ServiceRequest r1 = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .nurse(assignedNurse())
                .serviceDescription("desc").status(ServiceRequestStatus.COMPLETED)
                .latitude(LAT).longitude(LNG).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();
        when(serviceRequestRepository.findByProfile_User_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                eq(PATIENT_USER_ID), anyCollection())).thenReturn(List.of(r1));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.0010, 30.0010)));

        List<ServiceRequestHistoryResponse> result = service.listConfirmedHistory(PATIENT_USER_ID);

        assertEquals(1, result.size());
        ServiceRequestHistoryResponse response = result.get(0);
        assertEquals(REQ_ID, response.serviceRequestId());
        assertEquals(ST_ID, response.serviceTypeId());
        assertEquals("Home Nursing", response.serviceName());
        assertEquals(ServiceRequestStatus.COMPLETED, response.status());
        assertEquals(NURSE_ID, response.nurseId());
        assertEquals("Sara Hassan", response.nurseName());
        assertEquals("nurse-img", response.nurseProfileImageUrl());
        assertTrue(response.distanceKm() > 0 && response.distanceKm() < 0.2);
        assertEquals(120, response.estimatedDurationMinutes());
        assertEquals(NOW, response.createdAt());
        assertEquals(NOW_2, response.updatedAt());
    }

    @Test
    void listConfirmedHistory_edgeCases_coverNameAndNullPaths() {
        ServiceRequest rFullName = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .nurse(assignedNurse())
                .serviceDescription("d").status(ServiceRequestStatus.ACCEPTED)
                .latitude(LAT).longitude(LNG).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();

        User lastOnly = User.builder().id(UUID.randomUUID()).firstName(null).lastName("Smith").build();
        ServiceRequest rLastOnly = ServiceRequest.builder()
                .id(REQ_ID_2).profile(patientProfile()).serviceType(serviceType())
                .nurse(Nurse.builder().id(UUID.randomUUID()).user(lastOnly).build())
                .serviceDescription("d").status(ServiceRequestStatus.ACCEPTED)
                .latitude(null).longitude(null).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();

        User firstOnly = User.builder().id(UUID.randomUUID()).firstName("Ann").lastName(null).build();
        ServiceRequest rFirstOnly = ServiceRequest.builder()
                .id(UUID.randomUUID()).profile(patientProfile()).serviceType(serviceType())
                .nurse(Nurse.builder().id(UUID.randomUUID()).user(firstOnly).build())
                .serviceDescription("d").status(ServiceRequestStatus.ACCEPTED)
                .latitude(LAT).longitude(LNG).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();

        ServiceRequest rNoNames = ServiceRequest.builder()
                .id(UUID.randomUUID()).profile(patientProfile()).serviceType(serviceType())
                .nurse(Nurse.builder().id(UUID.randomUUID())
                        .user(User.builder().id(UUID.randomUUID()).firstName(null).lastName(null).build()).build())
                .serviceDescription("d").status(ServiceRequestStatus.IN_PROGRESS)
                .latitude(LAT).longitude(LNG).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();

        ServiceRequest rNoNurseUser = ServiceRequest.builder()
                .id(UUID.randomUUID()).profile(patientProfile()).serviceType(serviceType())
                .nurse(Nurse.builder().id(UUID.randomUUID()).build())
                .serviceDescription("d").status(ServiceRequestStatus.IN_PROGRESS)
                .latitude(LAT).longitude(null).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();

        ServiceRequest rNullLatitude = ServiceRequest.builder()
                .id(UUID.randomUUID()).profile(patientProfile()).serviceType(serviceType())
                .nurse(Nurse.builder().id(UUID.randomUUID())
                        .user(User.builder().id(UUID.randomUUID()).build()).build())
                .serviceDescription("d").status(ServiceRequestStatus.IN_PROGRESS)
                .latitude(null).longitude(LNG).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();

        ServiceRequest rNoTypeNoNurse = ServiceRequest.builder()
                .id(UUID.randomUUID()).profile(patientProfile()).serviceType(null).nurse(null)
                .serviceDescription("d").status(ServiceRequestStatus.COMPLETED)
                .latitude(LAT).longitude(LNG).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();

        when(serviceRequestRepository.findByProfile_User_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                eq(PATIENT_USER_ID), anyCollection()))
                .thenReturn(List.of(rFullName, rLastOnly, rFirstOnly, rNoNames, rNoNurseUser, rNoTypeNoNurse, rNullLatitude));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.0010, 30.0010)));

        List<ServiceRequestHistoryResponse> result = service.listConfirmedHistory(PATIENT_USER_ID);

        assertEquals(7, result.size());
        assertEquals("Sara Hassan", result.get(0).nurseName());
        assertNotNull(result.get(0).distanceKm());
        assertEquals("Smith", result.get(1).nurseName());
        assertNull(result.get(1).distanceKm());
        assertEquals("Ann", result.get(2).nurseName());
        assertNull(result.get(3).nurseName());
        assertNull(result.get(4).nurseName());
        assertNull(result.get(4).nurseProfileImageUrl());
        assertNull(result.get(5).serviceTypeId());
        assertNull(result.get(5).serviceName());
        assertNull(result.get(5).nurseId());
        assertNull(result.get(5).nurseName());
        assertNull(result.get(5).distanceKm());
        assertNull(result.get(5).estimatedDurationMinutes());
        assertNull(result.get(6).nurseName());
        assertNull(result.get(6).distanceKm());
    }

    @Test
    void listConfirmedHistory_nurseWithoutLiveLocation_distanceNull() {
        ServiceRequest r1 = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .nurse(assignedNurse())
                .serviceDescription("desc").status(ServiceRequestStatus.COMPLETED)
                .latitude(LAT).longitude(LNG).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();
        when(serviceRequestRepository.findByProfile_User_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                eq(PATIENT_USER_ID), anyCollection())).thenReturn(List.of(r1));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR)).thenReturn(Optional.empty());

        List<ServiceRequestHistoryResponse> result = service.listConfirmedHistory(PATIENT_USER_ID);

        assertEquals(1, result.size());
        assertNull(result.get(0).distanceKm());
    }

    // ------------------------------------------------------------------
    // listNurseHistory / toNurseHistoryResponse
    // ------------------------------------------------------------------

    @Test
    void listNurseHistory_notANurse_throwsResourceNotFound() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listNurseHistory(NURSE_USER_ID));
        verifyNoInteractions(serviceRequestRepository);
    }

    @Test
    void listNurseHistory_empty_returnsEmpty() {
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findByNurse_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                NURSE_ID, List.of(ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.IN_PROGRESS,
                        ServiceRequestStatus.COMPLETED)))
                .thenReturn(List.of());

        List<NurseRequestHistoryResponse> result = service.listNurseHistory(NURSE_USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void listNurseHistory_happy_returnsFullHistoryWithPatientAndPrice() {
        ServiceRequest r1 = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .nurse(assignedNurse())
                .serviceDescription("desc").status(ServiceRequestStatus.COMPLETED)
                .latitude(LAT).longitude(LNG).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findByNurse_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                eq(NURSE_ID), anyCollection())).thenReturn(List.of(r1));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.0010, 30.0010)));

        List<NurseRequestHistoryResponse> result = service.listNurseHistory(NURSE_USER_ID);

        assertEquals(1, result.size());
        NurseRequestHistoryResponse response = result.get(0);
        assertEquals(REQ_ID, response.serviceRequestId());
        assertEquals(ST_ID, response.serviceTypeId());
        assertEquals("Home Nursing", response.serviceName());
        assertEquals(120, response.estimatedDurationMinutes());
        assertEquals(PROFILE_ID, response.patientProfileId());
        assertEquals("Mona", response.patientFirstName());
        assertEquals("Ali", response.patientLastName());
        assertEquals("01012345678", response.patientPhoneNumber());
        assertEquals("patient-img", response.patientProfileImageUrl());
        assertEquals("desc", response.serviceDescription());
        assertEquals(ServiceRequestStatus.COMPLETED, response.status());
        assertNotNull(response.estimatedPrice());
        assertEquals(NOW, response.createdAt());
        assertEquals(NOW_2, response.updatedAt());
    }

    @Test
    void listNurseHistory_nurseWithoutLiveLocation_priceNull() {
        ServiceRequest r1 = ServiceRequest.builder()
                .id(REQ_ID).profile(patientProfile()).serviceType(serviceType())
                .nurse(assignedNurse())
                .serviceDescription("desc").status(ServiceRequestStatus.ACCEPTED)
                .latitude(LAT).longitude(LNG).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findByNurse_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                eq(NURSE_ID), anyCollection())).thenReturn(List.of(r1));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR)).thenReturn(Optional.empty());

        List<NurseRequestHistoryResponse> result = service.listNurseHistory(NURSE_USER_ID);

        assertEquals(1, result.size());
        assertNull(result.get(0).estimatedPrice());
    }

    @Test
    void listNurseHistory_edgeCases_coverNullPaths() {
        ServiceRequest rNoTypeNoProfile = ServiceRequest.builder()
                .id(REQ_ID).profile(null).serviceType(null).nurse(assignedNurse())
                .serviceDescription("d").status(ServiceRequestStatus.COMPLETED)
                .latitude(null).longitude(null).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();
        ServiceRequest rPartialUser = ServiceRequest.builder()
                .id(REQ_ID_2)
                .profile(Profile.builder().id(PROFILE_ID)
                        .user(User.builder().id(PATIENT_USER_ID).firstName(null).lastName(null)
                                .phoneNumber(null).build())
                        .profileImageUrl(" ").build())
                .serviceType(serviceType())
                .nurse(assignedNurse())
                .serviceDescription("d").status(ServiceRequestStatus.IN_PROGRESS)
                .latitude(LAT).longitude(LNG).isDeleted(false)
                .createdAt(NOW).updatedAt(NOW_2).build();
        when(nurseRepository.findByUser_Id(NURSE_USER_ID)).thenReturn(Optional.of(assignedNurse()));
        when(serviceRequestRepository.findByNurse_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                eq(NURSE_ID), anyCollection())).thenReturn(List.of(rNoTypeNoProfile, rPartialUser));
        when(webSocketPresenceService.getAvailableLocation(NURSE_USER_ID_STR))
                .thenReturn(Optional.of(new Point(31.0010, 30.0010)));

        List<NurseRequestHistoryResponse> result = service.listNurseHistory(NURSE_USER_ID);

        assertEquals(2, result.size());
        NurseRequestHistoryResponse first = result.get(0);
        assertNull(first.serviceTypeId());
        assertNull(first.serviceName());
        assertNull(first.estimatedDurationMinutes());
        assertNull(first.patientProfileId());
        assertNull(first.patientFirstName());
        assertNull(first.patientLastName());
        assertNull(first.patientPhoneNumber());
        assertNull(first.patientProfileImageUrl());
        assertNull(first.estimatedPrice());
        NurseRequestHistoryResponse second = result.get(1);
        assertEquals(PROFILE_ID, second.patientProfileId());
        assertNull(second.patientFirstName());
        assertNull(second.patientLastName());
        assertNull(second.patientPhoneNumber());
        assertNull(second.patientProfileImageUrl());
        assertNotNull(second.estimatedPrice());
    }

    // ------------------------------------------------------------------
    // constantTimeEquals private guard
    // ------------------------------------------------------------------

    @Test
    void constantTimeEquals_directCalls_coverNullAndMixed() {
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(service, "constantTimeEquals", (Object) null, "abc"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(service, "constantTimeEquals", "abc", (Object) null));
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(service, "constantTimeEquals", "abc", "abc"));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(service, "constantTimeEquals", "abc", "xyz"));
    }

    // ------------------------------------------------------------------
    // NearbyNurseMatcherImpl (real collaborator)
    // ------------------------------------------------------------------

    @Test
    void nearbyMatcher_nullOrEmptyLocations_returnsEmpty() {
        Set<UUID> required = Set.of(NURSE_ID);

        assertEquals(List.of(), nearbyNurseMatcher.findNearbyNurse(null, LAT, LNG, required));
        assertEquals(List.of(), nearbyNurseMatcher.findNearbyNurse(List.of(), LAT, LNG, required));
    }

    @Test
    void nearbyMatcher_nullRequiredServiceSet_keepsAllMatches() {
        NurseLocation close = new NurseLocation(NURSE_ID, LAT, LNG);
        NurseLocation far = new NurseLocation(NURSE_ID_2, new BigDecimal("30.5000"), new BigDecimal("31.5000"));

        List<NearbyNurse> result = nearbyNurseMatcher.findNearbyNurse(List.of(close, far), LAT, LNG, null);

        assertEquals(List.of(NURSE_ID), result.stream().map(NearbyNurse::nurseId).toList());
    }

    @Test
    void nearbyMatcher_filtersInvalidEntriesAndSortsByDistance() {
        NurseLocation nullEntry = null;
        NurseLocation noNurseId = new NurseLocation(null, LAT, LNG);
        NurseLocation noLat = new NurseLocation(NURSE_ID, null, LNG);
        NurseLocation noLng = new NurseLocation(NURSE_ID_2, LAT, null);
        NurseLocation farInSet = new NurseLocation(NURSE_ID_2, new BigDecimal("30.5000"), new BigDecimal("31.5000"));
        NurseLocation close1 = new NurseLocation(NURSE_ID, new BigDecimal("30.0010"), new BigDecimal("31.0000"));
        NurseLocation close2 = new NurseLocation(NURSE_ID, new BigDecimal("30.0000"), new BigDecimal("31.0000"));
        NurseLocation notInSet = new NurseLocation(NURSE_USER_ID, new BigDecimal("30.0000"), new BigDecimal("31.0000"));

        List<NearbyNurse> result = nearbyNurseMatcher.findNearbyNurse(
                java.util.Arrays.asList(nullEntry, noNurseId, noLat, noLng, farInSet, close1, close2, notInSet),
                LAT, LNG, Set.of(NURSE_ID, NURSE_ID_2));

        assertEquals(2, result.size());
        assertEquals(NURSE_ID, result.get(0).nurseId());
        assertEquals(0.0, result.get(0).distanceKm());
        assertEquals(NURSE_ID, result.get(1).nurseId());
        assertTrue(result.get(1).distanceKm() > 0 && result.get(1).distanceKm() < 0.2);
    }

    // ------------------------------------------------------------------
    // NurseLocationProviderImpl (real collaborator)
    // ------------------------------------------------------------------

    @Test
    void nurseLocationProvider_nullOrEmptyOnline_returnsEmpty() {
        when(webSocketPresenceService.getOnlineNurses()).thenReturn(null, new HashSet<String>());

        assertEquals(List.of(), nurseLocationProvider.getNurseLocations());
        assertEquals(List.of(), nurseLocationProvider.getNurseLocations());
    }

    @Test
    void nurseLocationProvider_filtersInvalidUsers_returnsValidOnly() {
        String badUuid = "not-a-uuid";
        String noNurseUser = UUID.randomUUID().toString();
        String pendingUser = UUID.randomUUID().toString();
        String noLocationUser = UUID.randomUUID().toString();
        String goodUser = UUID.randomUUID().toString();

        when(webSocketPresenceService.getOnlineNurses())
                .thenReturn(Set.of(badUuid, noNurseUser, pendingUser, noLocationUser, goodUser));
        when(nurseRepository.findByUser_Id(UUID.fromString(noNurseUser))).thenReturn(Optional.empty());
        when(nurseRepository.findByUser_Id(UUID.fromString(pendingUser)))
                .thenReturn(Optional.of(Nurse.builder().id(UUID.randomUUID()).user(nurseUser())
                        .verificationStatus(VerificationStatus.UNDER_REVIEW).build()));
        when(nurseRepository.findByUser_Id(UUID.fromString(noLocationUser)))
                .thenReturn(Optional.of(assignedNurse()));
        when(webSocketPresenceService.getAvailableLocation(noLocationUser)).thenReturn(Optional.empty());
        when(nurseRepository.findByUser_Id(UUID.fromString(goodUser))).thenReturn(Optional.of(assignedNurse()));
        when(webSocketPresenceService.getAvailableLocation(goodUser))
                .thenReturn(Optional.of(new Point(31.0000, 30.0000)));

        List<NurseLocation> result = nurseLocationProvider.getNurseLocations();

        assertEquals(1, result.size());
        assertEquals(NURSE_ID, result.get(0).nurseId());
        assertEquals(0, result.get(0).latitude().compareTo(new BigDecimal("30.0000")));
        assertEquals(0, result.get(0).longitude().compareTo(new BigDecimal("31.0000")));
    }
}