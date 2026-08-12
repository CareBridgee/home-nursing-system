package iti.jets.java.homenursing.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.testutil.BaseWebSocketIntegrationTest;
import iti.jets.java.homenursing.testutil.DevOtpAuth;
import iti.jets.java.homenursing.testutil.Json;
import iti.jets.java.homenursing.testutil.StompTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 3: WebSocket security suite - CONNECT auth (missing/invalid/expired token),
 * subscribe scoping (own /user OK, victim /user rejected, reservation/chat participant
 * rule) and SEND destination restrictions (clients may only send to /app/, presence
 * endpoints are nurse-only).
 */
class WebSocketSecurityIntegrationTest extends BaseWebSocketIntegrationTest {

    private static final Duration BUDGET = Duration.ofSeconds(10);
    private static final String LAT = "30.0444";
    private static final String LNG = "31.2357";

    @Test
    void connect_requiresValidAccessToken() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111500001");

        assertThrows(IllegalStateException.class,
                () -> new StompTestClient(wsUrl(), null).connect(BUDGET));
        assertThrows(IllegalStateException.class,
                () -> new StompTestClient(wsUrl(), "Bearer not-a-jwt").connect(BUDGET));
        assertThrows(IllegalStateException.class,
                () -> new StompTestClient(wsUrl(), "Bearer " + expiredAccessToken()).connect(BUDGET));

        try (StompTestClient client = new StompTestClient(wsUrl(), bearer(patient)).connect(BUDGET)) {
            assertTrue(client.isConnected(), "valid bearer must connect");
        }
    }

    @Test
    void subscribe_userQueue_scopedToOwner() throws Exception {
        DevOtpAuth.Tokens alice = DevOtpAuth.loginPatient(mvc, "+201111500002");
        DevOtpAuth.Tokens bob = DevOtpAuth.loginPatient(mvc, "+201111500003");
        String aliceId = currentUserId(alice);

        try (StompTestClient aliceClient = new StompTestClient(wsUrl(), bearer(alice)).connect(BUDGET)) {
            aliceClient.subscribe("/user/queue/errors");
            assertTrue(aliceClient.errors().isEmpty(),
                    "own /user/ subscription must be allowed: " + aliceClient.errors());
        }

        try (StompTestClient bobClient = new StompTestClient(wsUrl(), bearer(bob)).connect(BUDGET)) {
            bobClient.subscribe("/user/" + aliceId + "/queue/errors");
            await().atMost(BUDGET).until(() -> !bobClient.errors().isEmpty());
            assertTrue(!bobClient.errors().isEmpty(),
                    "victim /user/ subscription must be rejected: " + bobClient.errors());
        }
    }

    @Test
    void subscribe_reservationTopic_requiresParticipation() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111500004");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111500005");
        String nurseUserId = currentUserId(nurseTokens);
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111500005");
        DevOtpAuth.Tokens stranger = DevOtpAuth.loginPatient(mvc, "+201111500006");

        UUID reservationId = createReservation(patient, profileId, nurse.serviceTypeId(), nurseTokens);

        try (StompTestClient strangerClient = new StompTestClient(wsUrl(), bearer(stranger)).connect(BUDGET)) {
            strangerClient.subscribe("/topic/reservation/" + reservationId);
            await().atMost(BUDGET).until(() -> !strangerClient.errors().isEmpty());
            assertTrue(!strangerClient.errors().isEmpty(),
                    "non-participant reservation subscription must be rejected: " + strangerClient.errors());
        }

        try (StompTestClient strangerClient = new StompTestClient(wsUrl(), bearer(stranger)).connect(BUDGET)) {
            strangerClient.subscribe("/topic/chat/" + reservationId);
            await().atMost(BUDGET).until(() -> !strangerClient.errors().isEmpty());
            assertTrue(!strangerClient.errors().isEmpty(),
                    "non-participant chat subscription must be rejected: " + strangerClient.errors());
        }

        try (StompTestClient patientClient = new StompTestClient(wsUrl(), bearer(patient)).connect(BUDGET)) {
            patientClient.subscribe("/topic/reservation/" + reservationId);
            patientClient.subscribe("/topic/chat/" + reservationId);
            patientClient.subscribe("/user/queue/errors");
            assertTrue(patientClient.errors().isEmpty(),
                    "participant subscriptions must be allowed: " + patientClient.errors());
        }

        try (StompTestClient nurseClient = new StompTestClient(wsUrl(), bearer(nurseTokens)).connect(BUDGET)) {
            nurseClient.subscribe("/user/queue/nearby-request");
            nurseClient.subscribe("/topic/reservation/" + reservationId);
            assertTrue(nurseClient.errors().isEmpty(), "own user queue must be allowed: " + nurseClient.errors());
        }
    }

    @Test
    void send_restrictedToAppDestinations_andPresenceNurseOnly() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111500007");

        try (StompTestClient patientClient = new StompTestClient(wsUrl(), bearer(patient)).connect(BUDGET)) {
            patientClient.send("/topic/foo", "{}");
            await().atMost(BUDGET).until(() -> !patientClient.errors().isEmpty());
            assertTrue(!patientClient.errors().isEmpty(),
                    "SEND to /topic/ must be rejected: " + patientClient.errors());
        }

        try (StompTestClient patientClient = new StompTestClient(wsUrl(), bearer(patient)).connect(BUDGET)) {
            patientClient.send("/user/" + currentUserId(patient) + "/queue/x", "{}");
            await().atMost(BUDGET).until(() -> !patientClient.errors().isEmpty());
            assertTrue(!patientClient.errors().isEmpty(),
                    "SEND to /user/ must be rejected: " + patientClient.errors());
        }

        try (StompTestClient patientClient = new StompTestClient(wsUrl(), bearer(patient)).connect(BUDGET)) {
            patientClient.send("/app/heartbeat", "{}");
            await().atMost(BUDGET).until(() -> !patientClient.errors().isEmpty());
            assertTrue(!patientClient.errors().isEmpty(),
                    "non-nurse presence SEND must be rejected: " + patientClient.errors());
        }
    }

    private String expiredAccessToken() {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .claim("type", "access")
                .claim("role", "USER")
                .setIssuedAt(new Date(now - 3_600_000))
                .setExpiration(new Date(now - 3_000_000))
                .signWith(Keys.hmacShaKeyFor(
                                "test-jwt-secret-for-tests-1234567890".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }

    private String currentUserId(DevOtpAuth.Tokens tokens) throws Exception {
        String body = mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, iti.jets.java.homenursing.dto.user.UserResponse.class).getId().toString();
    }

    private UUID defaultProfileId(DevOtpAuth.Tokens tokens) throws Exception {
        String body = mvc.perform(get("/api/v1/profiles/default").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, iti.jets.java.homenursing.dto.profile.ProfileResponse.class).getId();
    }

    private UUID seedServiceType() throws Exception {
        String stamp = "W" + System.nanoTime();
        String body = mvc.perform(multipart("/api/v1/admin/catalog/service-types")
                        .file(image("file"))
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, DevOtpAuth.ADMIN_KEY)
                        .param("name", "WS Security " + stamp)
                        .param("description", "basic")
                        .param("category", "NURSING")
                        .param("estimatedDurationMinutes", "90")
                        .param("basePrice", "400.00"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, iti.jets.java.homenursing.dto.catalog.ServiceTypeResponse.class).id();
    }

    private record NurseAndId(String nurseId, UUID serviceTypeId) {
    }

    private NurseAndId registeredNurse(DevOtpAuth.Tokens nurseTokens, String phone) throws Exception {
        UUID serviceTypeId = seedServiceType();
        String nationalId = "30000000" + phone.substring(phone.length() - 6);
        String body = mvc.perform(multipart("/api/v1/nurses/register")
                        .file(image("nationalIdFront"))
                        .file(image("nationalIdBack"))
                        .file(image("licenseImage"))
                        .file(image("professionalCertificate"))
                        .file(image("profileImage"))
                        .header("Authorization", bearer(nurseTokens))
                        .param("nationalId", nationalId)
                        .param("licenseNumber", "LIC-" + nationalId)
                        .param("specialization", "General Nursing")
                        .param("yearsOfExperience", "5")
                        .param("bio", "Test nurse"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String nurseId = Json.read(body, NurseResponse.class).getId().toString();
        mvc.perform(patch("/api/v1/admin/nurses/{nurseId}/approve", nurseId)
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, DevOtpAuth.ADMIN_KEY))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/nurses/{nurseId}/services", nurseId)
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(List.of(Map.of("serviceTypeId", serviceTypeId.toString())))))
                .andExpect(status().isOk());
        return new NurseAndId(nurseId, serviceTypeId);
    }

    private UUID createReservation(DevOtpAuth.Tokens patient, UUID profileId, UUID serviceTypeId,
                                   DevOtpAuth.Tokens nurseTokens) throws Exception {
        String body = mvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "profileId", profileId.toString(),
                                "serviceTypeId", serviceTypeId.toString(),
                                "latitude", LAT,
                                "longitude", LNG,
                                "preferredDate", LocalDate.now().plusDays(3).toString(),
                                "preferredTime", "11:00"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID requestId = Json.read(body, NearbyServiceRequestResponse.class).serviceRequestId();

        String offerBody = mvc.perform(post("/api/v1/nurse-offers")
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", requestId.toString(),
                                "proposedPrice", "500.00",
                                "proposedDate", LocalDate.now().plusDays(4).toString(),
                                "proposedTime", "10:00"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID offerId = Json.read(offerBody, iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse.class).id();
        mvc.perform(patch("/api/v1/nurse-offers/{id}/accept", offerId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk());
        return requestId;
    }
}
