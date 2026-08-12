package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.ChatMessage;
import iti.jets.java.homenursing.entity.FailedStep;
import iti.jets.java.homenursing.entity.Notification;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseOffer;
import iti.jets.java.homenursing.entity.NurseRejectionDetail;
import iti.jets.java.homenursing.entity.NurseService;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ReviewRating;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.NotificationType;
import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class NurseDomainRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private NurseRepository nurseRepository;
    @Autowired
    private NurseServiceRepository nurseServiceRepository;
    @Autowired
    private NurseRejectionDetailRepository nurseRejectionDetailRepository;
    @Autowired
    private NurseOfferRepository nurseOfferRepository;
    @Autowired
    private ReviewRatingRepository reviewRatingRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private ServiceTypeRepository serviceTypeRepository;
    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    private User user(String phone) {
        return userRepository.save(User.builder().phoneNumber(phone).firstName("N").lastName("U")
                .isDeleted(false).build());
    }

    private Nurse nurse(User user) {
        return nurseRepository.save(Nurse.builder().user(user)
                
                .nationalId("30401010100001")
                .verificationStatus(VerificationStatus.APPROVED)
                .build());
    }

    private Profile profile(User user) {
        return profileRepository.save(Profile.builder().user(user)
                .firstName("P").lastName("T").isPrimary(true).isDeleted(false).build());
    }

    private ServiceType serviceType(String name) {
        return serviceTypeRepository.save(ServiceType.builder().name(name)
                .basePrice(java.math.BigDecimal.valueOf(100)).build());
    }

    private ServiceRequest request(Profile profile, Nurse nurse, ServiceType type, ServiceRequestStatus status) {
        return serviceRequestRepository.save(ServiceRequest.builder()
                .profile(profile).nurse(nurse).serviceType(type).status(status)
                .isDeleted(false).createdAt(LocalDateTime.now()).build());
    }

    @Test
    void nurse_allQueries() {
        User user = user("+201100000001");
        Nurse nurse = nurse(user);

        assertTrue(nurseRepository.existsByUser_Id(user.getId()));
        assertFalse(nurseRepository.existsByUser_Id(UUID.randomUUID()));
        assertTrue(nurseRepository.existsByNationalId("30401010100001"));
        assertFalse(nurseRepository.existsByNationalId("00000000000000"));
        assertEquals(nurse.getId(), nurseRepository.findByUser_Id(user.getId()).orElseThrow().getId());
        assertEquals(nurse.getId(), nurseRepository.findWithUserById(nurse.getId()).orElseThrow().getId());
        assertEquals(1, nurseRepository.findByVerificationStatus(VerificationStatus.APPROVED).size());
        assertTrue(nurseRepository.findByVerificationStatus(VerificationStatus.UNDER_REVIEW).isEmpty());
    }

    @Test
    void nurseService_allQueries() {
        User user = user("+201100000002");
        Nurse nurse = nurse(user);
        ServiceType type1 = serviceType("General Nursing");
        ServiceType type2 = serviceType("Physiotherapy");
        NurseService active = nurseServiceRepository.save(NurseService.builder()
                .nurse(nurse).serviceType(type1).isActive(true).build());
        nurseServiceRepository.save(NurseService.builder()
                .nurse(nurse).serviceType(type2).isActive(false).build());

        assertEquals(2, nurseServiceRepository.findAllByNurse_Id(nurse.getId()).size());
        assertEquals(1, nurseServiceRepository.findByNurse_IdAndIsActiveTrue(nurse.getId()).size());
        assertEquals(active.getId(), nurseServiceRepository
                .findByNurse_IdAndServiceType_Id(nurse.getId(), type1.getId()).orElseThrow().getId());
        assertTrue(nurseServiceRepository.findByNurse_IdAndServiceType_Id(nurse.getId(), UUID.randomUUID()).isEmpty());
        assertEquals(1, nurseServiceRepository.findByServiceType_IdAndIsActiveTrue(type1.getId()).size());
        assertEquals(1, nurseServiceRepository
                .findByServiceType_NameContainingIgnoreCaseAndIsActiveTrue("nursing").size());
        assertEquals(0, nurseServiceRepository
                .findByServiceType_NameContainingIgnoreCaseAndIsActiveTrue("physio").size());
    }

    @Test
    void nurseRejectionDetail_findAndDelete() {
        User user = user("+201100000003");
        Nurse nurse = nurse(user);
        NurseRejectionDetail detail = nurseRejectionDetailRepository.save(NurseRejectionDetail.builder()
                .nurse(nurse)
                .overallReason("Docs missing")
                .failedSteps(List.of(new FailedStep("LICENSE", "Missing")))
                .build());

        assertEquals("Docs missing", nurseRejectionDetailRepository.findByNurseId(nurse.getId()).orElseThrow().getOverallReason());
        assertTrue(nurseRejectionDetailRepository.findByNurseId(UUID.randomUUID()).isEmpty());

        nurseRejectionDetailRepository.deleteByNurseId(nurse.getId());
        assertTrue(nurseRejectionDetailRepository.findByNurseId(nurse.getId()).isEmpty());
    }

    @Test
    void nurseOffer_allQueries() {
        User patientUser = user("+201100000004");
        Profile profile = profile(patientUser);
        Nurse nurse = nurse(user("+201100000005"));
        ServiceRequest sr = request(profile, nurse, serviceType("General Nursing"), ServiceRequestStatus.PENDING);

        NurseOffer first = nurseOfferRepository.save(NurseOffer.builder()
                .serviceRequest(sr).nurse(nurse).status(NurseOfferStatus.PENDING)
                .proposedPrice(java.math.BigDecimal.valueOf(200L)).isDeleted(false).createdAt(LocalDateTime.now()).build());
        nurseOfferRepository.save(NurseOffer.builder()
                .serviceRequest(sr).nurse(nurse).status(NurseOfferStatus.ACCEPTED)
                .isDeleted(true).createdAt(LocalDateTime.of(2025, 1, 1, 0, 0)).build());

        List<NurseOffer> offers = nurseOfferRepository.findByServiceRequest_IdAndIsDeletedFalseOrderByCreatedAtDesc(sr.getId());
        assertEquals(1, offers.size());
        assertEquals(first.getId(), offers.get(0).getId());
        assertEquals(first.getId(), nurseOfferRepository.findByIdAndIsDeletedFalse(first.getId()).orElseThrow().getId());
        assertTrue(nurseOfferRepository.findByIdAndIsDeletedFalse(UUID.randomUUID()).isEmpty());
        assertTrue(nurseOfferRepository
                .existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalseAndStatus(sr.getId(), nurse.getUser().getId(), NurseOfferStatus.PENDING));
        assertFalse(nurseOfferRepository
                .existsByServiceRequest_IdAndNurse_User_IdAndIsDeletedFalseAndStatus(sr.getId(), nurse.getUser().getId(), NurseOfferStatus.ACCEPTED));
    }

    @Test
    void reviewRating_allQueries() {
        User patientUser = user("+201100000006");
        Profile profile = profile(patientUser);
        Nurse nurse = nurse(user("+201100000007"));
        ServiceRequest sr = request(profile, nurse, serviceType("General Nursing"), ServiceRequestStatus.COMPLETED);
        ReviewRating rating = reviewRatingRepository.save(ReviewRating.builder()
                .serviceRequest(sr).profile(profile).nurse(nurse)
                .rating(5).reviewText("Great").isAnonymous(false).createdAt(LocalDateTime.now()).build());

        assertEquals(1, reviewRatingRepository.findByProfileId(profile.getId()).size());
        assertEquals(1, reviewRatingRepository
                .findByNurseId(nurse.getId(), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getTotalElements());
        assertEquals(rating.getId(), reviewRatingRepository.findByServiceRequestId(sr.getId()).orElseThrow().getId());
        assertTrue(reviewRatingRepository.existsByServiceRequestId(sr.getId()));
    }

    @Test
    void notification_allQueries() {
        User user = user("+201100000008");
        Notification older = notificationRepository.save(Notification.builder()
                .user(user).type(NotificationType.MESSAGE).title("Old").message("old msg")
                .isRead(false).relatedEntityType("SERVICE_REQUEST").relatedEntityId(UUID.randomUUID())
                .build());
        Notification newer = notificationRepository.save(Notification.builder()
                .user(user).type(NotificationType.BOOKING).title("New").message("new msg")
                .isRead(true).relatedEntityType("SERVICE_REQUEST").relatedEntityId(UUID.randomUUID())
                .build());

        List<Notification> all = notificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
        assertEquals(2, all.size());
        assertEquals(older.getId(), notificationRepository
                .findByUser_IdAndId(user.getId(), older.getId()).orElseThrow().getId());
        assertTrue(notificationRepository.findByUser_IdAndId(user.getId(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void chatMessage_allQueries() {
        User patientUser = user("+201100000009");
        Profile profile = profile(patientUser);
        ServiceRequest sr = request(profile, null, serviceType("General Nursing"), ServiceRequestStatus.PENDING);
        chatMessageRepository.save(ChatMessage.builder()
                .serviceRequest(sr).senderUserId(UUID.randomUUID()).content("Hi").build());
        chatMessageRepository.save(ChatMessage.builder()
                .serviceRequest(sr).senderUserId(UUID.randomUUID()).content("Hello").build());
        chatMessageRepository.save(ChatMessage.builder()
                .serviceRequest(sr).senderUserId(UUID.randomUUID()).content("Bye").build());

        List<ChatMessage> all = chatMessageRepository.findByServiceRequest_IdOrderByCreatedAtAsc(sr.getId());
        assertEquals(3, all.size());
        List<ChatMessage> recent = chatMessageRepository
                .findByServiceRequest_IdAndCreatedAtAfterOrderByCreatedAtAsc(sr.getId(), LocalDateTime.now().minusDays(1));
        assertEquals(3, recent.size());
    }
}
