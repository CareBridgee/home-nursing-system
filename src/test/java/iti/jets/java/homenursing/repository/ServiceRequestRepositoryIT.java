package iti.jets.java.homenursing.repository;

import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.NurseOffer;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.ServiceRequest;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.NurseOfferStatus;
import iti.jets.java.homenursing.entity.enums.ServiceRequestStatus;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
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
class ServiceRequestRepositoryIT {

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
    private NurseOfferRepository nurseOfferRepository;
    @Autowired
    private ServiceTypeRepository serviceTypeRepository;
    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    private User user(String phone) {
        return userRepository.save(User.builder().phoneNumber(phone).firstName("N").lastName("U")
                .isDeleted(false).build());
    }

    private Profile profile(User user) {
        return profileRepository.save(Profile.builder().user(user)
                .firstName("P").lastName("T").isPrimary(true).isDeleted(false).build());
    }

    private Nurse nurse(User user) {
        return nurseRepository.save(Nurse.builder().user(user)
                .nationalId(UUID.randomUUID().toString().substring(0, 14))
                .verificationStatus(VerificationStatus.APPROVED).build());
    }

    private ServiceType serviceType(String name) {
        return serviceTypeRepository.save(ServiceType.builder().name(name)
                .basePrice(java.math.BigDecimal.valueOf(100)).build());
    }

    private ServiceRequest request(Profile profile, Nurse nurse, ServiceType type, ServiceRequestStatus status,
                                   boolean deleted, LocalDateTime createdAt) {
        return serviceRequestRepository.save(ServiceRequest.builder()
                .profile(profile).nurse(nurse).serviceType(type).status(status)
                .isDeleted(deleted).createdAt(createdAt).build());
    }

    @Test
    void findByProfileUser_ordersByCreatedAtDesc_andFiltersStatus() {
        User patient = user("+201200000001");
        Profile p = profile(patient);
        Nurse n = nurse(user("+201200000002"));
        ServiceType t = serviceType("General Nursing");
        ServiceRequest old = request(p, n, t, ServiceRequestStatus.COMPLETED, false, LocalDateTime.of(2025, 1, 1, 0, 0));
        ServiceRequest recent = request(p, n, t, ServiceRequestStatus.PENDING, false, LocalDateTime.of(2026, 1, 1, 0, 0));
        request(p, n, t, ServiceRequestStatus.PENDING, true, LocalDateTime.of(2026, 2, 1, 0, 0));

        List<ServiceRequest> all = serviceRequestRepository
                .findByProfile_User_IdAndIsDeletedFalseOrderByCreatedAtDesc(patient.getId());
        assertEquals(2, all.size());
        assertEquals(recent.getId(), all.get(0).getId());

        List<ServiceRequest> byProfile = serviceRequestRepository
                .findByProfile_IdAndIsDeletedFalseOrderByCreatedAtDesc(p.getId());
        assertEquals(2, byProfile.size());

        assertTrue(serviceRequestRepository
                .existsByProfile_IdAndIsDeletedFalseAndStatusIn(p.getId(), List.of(ServiceRequestStatus.PENDING)));
        assertFalse(serviceRequestRepository
                .existsByProfile_IdAndIsDeletedFalseAndStatusIn(p.getId(), List.of(ServiceRequestStatus.ACCEPTED)));
        assertTrue(serviceRequestRepository
                .existsByProfile_IdAndIsDeletedFalseAndStatusIn(p.getId(), List.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.ACCEPTED)));
        assertEquals(old.getId(), serviceRequestRepository.findByIdAndIsDeletedFalse(old.getId()).orElseThrow().getId());
        assertTrue(serviceRequestRepository.findByIdAndIsDeletedFalse(UUID.randomUUID()).isEmpty());
    }

    @Test
    void statusBasedFilters_forNurseAndProfile() {
        User patient = user("+201200000003");
        Profile p = profile(patient);
        Nurse n = nurse(user("+201200000004"));
        ServiceType t = serviceType("General Nursing");
        ServiceRequest active = request(p, n, t, ServiceRequestStatus.IN_PROGRESS, false, LocalDateTime.now());
        request(p, n, t, ServiceRequestStatus.COMPLETED, false, LocalDateTime.now());

        assertTrue(serviceRequestRepository
                .existsByNurse_IdAndIsDeletedFalseAndStatusIn(n.getId(), List.of(ServiceRequestStatus.IN_PROGRESS)));
        assertFalse(serviceRequestRepository
                .existsByNurse_IdAndIsDeletedFalseAndStatusIn(n.getId(), List.of(ServiceRequestStatus.PENDING)));
        assertTrue(serviceRequestRepository.existsByProfile_IdAndNurse_User_IdAndIsDeletedFalse(p.getId(), n.getUser().getId()));
        assertFalse(serviceRequestRepository
                .existsByProfile_IdAndNurse_User_IdAndIsDeletedFalse(p.getId(), UUID.randomUUID()));
        assertTrue(serviceRequestRepository.existsByProfile_IdAndNurse_User_IdAndIsDeletedFalseAndStatusIn(
                p.getId(), n.getUser().getId(), List.of(ServiceRequestStatus.IN_PROGRESS)));
        assertFalse(serviceRequestRepository.existsByProfile_IdAndNurse_User_IdAndIsDeletedFalseAndStatusIn(
                p.getId(), n.getUser().getId(), List.of(ServiceRequestStatus.PENDING)));
        assertEquals(active.getId(), serviceRequestRepository
                .findFirstByNurse_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                        n.getId(), List.of(ServiceRequestStatus.IN_PROGRESS, ServiceRequestStatus.PENDING)).orElseThrow().getId());
        assertEquals(active.getId(), serviceRequestRepository
                .findFirstByProfile_User_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                        patient.getId(), List.of(ServiceRequestStatus.IN_PROGRESS)).orElseThrow().getId());
    }

    @Test
    void unassignedAndStatusHistoryQueries() {
        User patient = user("+201200000005");
        Profile p = profile(patient);
        ServiceType t = serviceType("General Nursing");
        ServiceRequest openPending = request(p, null, t, ServiceRequestStatus.PENDING, false, LocalDateTime.of(2026, 1, 1, 0, 0));
        request(p, null, t, ServiceRequestStatus.COMPLETED, false, LocalDateTime.of(2026, 2, 1, 0, 0));
        request(p, null, t, ServiceRequestStatus.PENDING, true, LocalDateTime.of(2026, 3, 1, 0, 0));

        List<ServiceRequest> unassigned = serviceRequestRepository
                .findByProfile_User_IdAndIsDeletedFalseAndStatusInAndNurseNullOrderByCreatedAtDesc(
                        patient.getId(), List.of(ServiceRequestStatus.PENDING));
        assertEquals(1, unassigned.size());
        assertEquals(openPending.getId(), unassigned.get(0).getId());

        List<ServiceRequest> history = serviceRequestRepository
                .findByProfile_User_IdAndIsDeletedFalseAndStatusInOrderByCreatedAtDesc(
                        patient.getId(), List.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.COMPLETED));
        assertEquals(2, history.size());
        assertEquals("COMPLETED", history.get(0).getStatus().name());
    }

    @Test
    void findWithDetailsById_loadsAssociations() {
        User patient = user("+201200000006");
        Profile p = profile(patient);
        Nurse n = nurse(user("+201200000007"));
        ServiceType t = serviceType("General Nursing");
        ServiceRequest sr = request(p, n, t, ServiceRequestStatus.PENDING, false, LocalDateTime.now());

        ServiceRequest loaded = serviceRequestRepository.findWithDetailsById(sr.getId()).orElseThrow();

        assertEquals(p.getId(), loaded.getProfile().getId());
        assertEquals(patient.getId(), loaded.getProfile().getUser().getId());
        assertEquals(n.getId(), loaded.getNurse().getId());
        assertEquals(n.getUser().getId(), loaded.getNurse().getUser().getId());
        assertEquals(t.getId(), loaded.getServiceType().getId());
        assertTrue(serviceRequestRepository.findWithDetailsById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void findOpenRequestsForServiceTypes_filtersCorrectly() {
        User patient = user("+201200000008");
        Profile p = profile(patient);
        Nurse n = nurse(user("+201200000009"));
        ServiceType t1 = serviceType("General Nursing");
        ServiceType t2 = serviceType("Physiotherapy");
        List<UUID> typeIds = List.of(t1.getId());

        ServiceRequest open = request(p, null, t1, ServiceRequestStatus.PENDING, false, LocalDateTime.of(2026, 1, 1, 0, 0));
        request(p, null, t1, ServiceRequestStatus.PENDING, true, LocalDateTime.of(2026, 1, 2, 0, 0));
        request(p, n, t1, ServiceRequestStatus.PENDING, false, LocalDateTime.of(2026, 1, 3, 0, 0));
        request(p, null, t1, ServiceRequestStatus.COMPLETED, false, LocalDateTime.of(2026, 1, 4, 0, 0));
        request(p, null, t2, ServiceRequestStatus.PENDING, false, LocalDateTime.of(2026, 1, 5, 0, 0));

        List<ServiceRequest> openRequests = serviceRequestRepository.findOpenRequestsForServiceTypes(
                typeIds, List.of(ServiceRequestStatus.PENDING));

        assertEquals(1, openRequests.size());
        assertEquals(open.getId(), openRequests.get(0).getId());
    }

    @Test
    void isParticipant_ownerNurseAndOfferHolder() {
        User patient = user("+201200000010");
        Profile p = profile(patient);
        Nurse assignedNurse = nurse(user("+201200000011"));
        Nurse offeringNurse = nurse(user("+201200000012"));
        ServiceType t = serviceType("General Nursing");
        ServiceRequest sr = request(p, assignedNurse, t, ServiceRequestStatus.PENDING, false, LocalDateTime.now());
        nurseOfferRepository.save(NurseOffer.builder()
                .serviceRequest(sr).nurse(offeringNurse).status(NurseOfferStatus.PENDING)
                .proposedPrice(java.math.BigDecimal.valueOf(200L)).isDeleted(false).createdAt(LocalDateTime.now()).build());

        assertTrue(serviceRequestRepository.isParticipant(sr.getId(), patient.getId()));
        assertTrue(serviceRequestRepository.isParticipant(sr.getId(), assignedNurse.getUser().getId()));
        assertTrue(serviceRequestRepository.isParticipant(sr.getId(), offeringNurse.getUser().getId()));
        assertFalse(serviceRequestRepository.isParticipant(sr.getId(), UUID.randomUUID()));
    }

    @Test
    void isParticipant_deletedRequest_returnsFalse() {
        User patient = user("+201200000013");
        Profile p = profile(patient);
        ServiceType t = serviceType("General Nursing");
        ServiceRequest sr = request(p, null, t, ServiceRequestStatus.PENDING, true, LocalDateTime.now());

        assertFalse(serviceRequestRepository.isParticipant(sr.getId(), patient.getId()));
    }
}
