package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.reservation.ReservationEvent;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.dto.ws.SocketErrorPayload;
import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import iti.jets.java.homenursing.testutil.BaseWebSocketIntegrationTest;
import iti.jets.java.homenursing.testutil.DevOtpAuth;
import iti.jets.java.homenursing.testutil.Json;
import iti.jets.java.homenursing.testutil.StompTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 3: WebSocket flow suite - offer lifecycle events on /topic/reservation/{id}
 * (create/update/accept), chat fan-out on /topic/chat/{id}, the error-queue payload shapes
 * (RESOURCE_NOT_FOUND, BAD_REQUEST, VALIDATION) and the nearby-request push to
 * /user/{id}/queue/nearby-request driven by WS presence availability.
 */
class WebSocketFlowIntegrationTest extends BaseWebSocketIntegrationTest {

    @Autowired
    private WebSocketPresenceService presenceService;

    private static final Duration BUDGET = Duration.ofSeconds(10);
    private static final String LAT = "30.0444";
    private static final String LNG = "31.2357";

    @Test
    void ws_offer_lifecycle_events_and_errors() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111520001");
        UUID profileId = defaultProfileId(patient);
        String patientUserId = userIdOf(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111520002");
        String nurseUserId = userIdOf(nurseTokens);
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111520002");

        String requestBody = mvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "profileId", profileId.toString(),
                                "serviceTypeId", nurse.serviceTypeId().toString(),
                                "latitude", LAT,
                                "longitude", LNG,
                                "preferredDate", LocalDate.now().plusDays(3).toString(),
                                "preferredTime", "11:00"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID reservationId = Json.read(requestBody, NearbyServiceRequestResponse.class).serviceRequestId();

        String offerBody = mvc.perform(post("/api/v1/nurse-offers")
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", reservationId.toString(),
                                "proposedPrice", "500.00",
                                "proposedDate", LocalDate.now().plusDays(4).toString(),
                                "proposedTime", "10:00"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID offerId = Json.read(offerBody, iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse.class).id();

        try (StompTestClient nurseClient = new StompTestClient(wsUrl(), bearer(nurseTokens)).connect(BUDGET)) {
            String topicSub = nurseClient.subscribe("/topic/reservation/" + reservationId);
            String errorsSub = nurseClient.subscribe("/user/queue/errors");

            nurseClient.send("/app/reservation/offer/update",
                    Json.write(Map.of("offerId", offerId.toString(), "proposedPrice", "600.00")));
            ReservationEvent updated = Json.read(nurseClient.awaitMessage(topicSub, BUDGET), ReservationEvent.class);
            assertThat(updated.type(), is("OFFER_UPDATED"));
            assertThat(updated.reservationId(), is(reservationId));

            nurseClient.send("/app/reservation/offer/accept", Json.write(Map.of()));
            SocketErrorPayload validation = Json.read(nurseClient.awaitMessage(errorsSub, BUDGET),
                    SocketErrorPayload.class);
            assertThat(validation.code(), is("VALIDATION"));

            nurseClient.send("/app/reservation/offer/create",
                    Json.write(Map.of(
                            "serviceRequestId", reservationId.toString(),
                            "proposedPrice", "700.00",
                            "proposedDate", LocalDate.now().plusDays(4).toString(),
                            "proposedTime", "12:00")));
            SocketErrorPayload duplicate = Json.read(nurseClient.awaitMessage(errorsSub, BUDGET),
                    SocketErrorPayload.class);
            assertThat(duplicate.code(), is("BAD_REQUEST"));
        }

        try (StompTestClient patientClient = new StompTestClient(wsUrl(), bearer(patient)).connect(BUDGET)) {
            String topicSub = patientClient.subscribe("/topic/reservation/" + reservationId);
            String errorsSub = patientClient.subscribe("/user/queue/errors");

            patientClient.send("/app/reservation/offer/accept",
                    Json.write(Map.of("offerId", offerId.toString())));
            ReservationEvent accepted = Json.read(patientClient.awaitMessage(topicSub, BUDGET),
                    ReservationEvent.class);
            assertThat(accepted.type(), is("OFFER_ACCEPTED"));

            patientClient.send("/app/reservation/cancel",
                    Json.write(Map.of("serviceRequestId", UUID.randomUUID().toString())));
            SocketErrorPayload notFound = Json.read(patientClient.awaitMessage(errorsSub, BUDGET),
                    SocketErrorPayload.class);
            assertThat(notFound.code(), is("RESOURCE_NOT_FOUND"));
        }
    }

    @Test
    void ws_chat_send_fanout_to_participants() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111520003");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111520004");
        String nurseUserId = userIdOf(nurseTokens);
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111520004");
        UUID reservationId = createAcceptedReservation(patient, profileId, nurse.serviceTypeId(), nurseTokens);

        try (StompTestClient patientClient = new StompTestClient(wsUrl(), bearer(patient)).connect(BUDGET)) {
            String chatSub = patientClient.subscribe("/topic/chat/" + reservationId);

            try (StompTestClient nurseClient = new StompTestClient(wsUrl(), bearer(nurseTokens)).connect(BUDGET)) {
                nurseClient.send("/app/chat/" + reservationId + "/send",
                        Json.write(Map.of("content", "ws hello")));
            }

            String messagePayload = patientClient.awaitMessage(chatSub, BUDGET);
            assertThat(messagePayload, containsString("ws hello"));
            assertThat(messagePayload, containsString(nurseUserId));
        }
    }

    @Test
    void nearby_push_via_ws_availability_and_rest_listings() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111520005");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111520006");
        String nurseUserId = userIdOf(nurseTokens);
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111520006");

        try (StompTestClient nurseClient = new StompTestClient(wsUrl(), bearer(nurseTokens)).connect(BUDGET)) {
            String nearbySub = nurseClient.subscribe("/user/queue/nearby-request");
            nurseClient.send("/app/heartbeat", "{}");
            await().atMost(BUDGET).until(() -> presenceService.getOnlineNurses().contains(nurseUserId));
            nurseClient.send("/app/reservation/availability",
                    Json.write(Map.of("available", true, "lat", LAT, "lng", LNG)));
            await().atMost(BUDGET).until(() -> presenceService.getAvailableLocation(nurseUserId).isPresent());

            String requestBody = mvc.perform(post("/api/v1/service-requests")
                            .header("Authorization", bearer(patient))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Json.write(Map.of(
                                    "profileId", profileId.toString(),
                                    "serviceTypeId", nurse.serviceTypeId().toString(),
                                    "latitude", LAT,
                                    "longitude", LNG,
                                    "preferredDate", LocalDate.now().plusDays(3).toString(),
                                    "preferredTime", "11:00"))))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            UUID reservationId = Json.read(requestBody, NearbyServiceRequestResponse.class).serviceRequestId();

            String pushPayload = nurseClient.awaitMessage(nearbySub, BUDGET);
            assertThat(pushPayload, containsString(reservationId.toString()));

            mvc.perform(get("/api/v1/service-requests/{id}/nearby-nurses", reservationId)
                            .header("Authorization", bearer(patient)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].nurseId", is(nurse.nurseId())));

            mvc.perform(get("/api/v1/service-requests/nearby")
                            .header("Authorization", bearer(nurseTokens)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.serviceRequestId == '" + reservationId + "')]", hasSize(1)));
        }
    }

    @Test
    void ws_offer_actions_create_counter_withdraw_reject_list_cancel() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111520007");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111520008");
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111520008");
        UUID reservationId = createRequest(patient, profileId, nurse.serviceTypeId());

        String offerBody = mvc.perform(post("/api/v1/nurse-offers")
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", reservationId.toString(),
                                "proposedPrice", "500.00",
                                "proposedDate", LocalDate.now().plusDays(4).toString(),
                                "proposedTime", "10:00"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String offer1Id = Json.read(offerBody, iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse.class)
                .id().toString();

        try (StompTestClient nurseClient = new StompTestClient(wsUrl(), bearer(nurseTokens)).connect(BUDGET);
             StompTestClient patientClient = new StompTestClient(wsUrl(), bearer(patient)).connect(BUDGET)) {
            String topicSub = nurseClient.subscribe("/topic/reservation/" + reservationId);
            String errorsSub = nurseClient.subscribe("/user/queue/errors");

            patientClient.send("/app/reservation/offer/counter",
                    Json.write(Map.of("offerId", offer1Id, "proposedPrice", "450.00")));
            ReservationEvent countered = Json.read(nurseClient.awaitMessage(topicSub, BUDGET), ReservationEvent.class);
            assertThat(countered.type(), is("OFFER_COUNTERED"));

            nurseClient.send("/app/reservation/offer/withdraw", Json.write(Map.of("offerId", offer1Id)));
            ReservationEvent withdrawn = Json.read(nurseClient.awaitMessage(topicSub, BUDGET), ReservationEvent.class);
            assertThat(withdrawn.type(), is("OFFER_WITHDRAWN"));

            nurseClient.send("/app/reservation/offer/create",
                    Json.write(Map.of(
                            "serviceRequestId", reservationId.toString(),
                            "proposedPrice", "600.00",
                            "proposedDate", LocalDate.now().plusDays(4).toString(),
                            "proposedTime", "11:00")));
            ReservationEvent created2 = Json.read(nurseClient.awaitMessage(topicSub, BUDGET), ReservationEvent.class);
            assertThat(created2.type(), is("OFFER_CREATED"));
            String offer2Id = offerIdOf(created2);

            patientClient.send("/app/reservation/offer/reject", Json.write(Map.of("offerId", offer2Id)));
            ReservationEvent rejected = Json.read(nurseClient.awaitMessage(topicSub, BUDGET), ReservationEvent.class);
            assertThat(rejected.type(), is("OFFER_REJECTED"));

            patientClient.send("/app/reservation/offers/list",
                    Json.write(Map.of("serviceRequestId", reservationId.toString())));
            ReservationEvent list = Json.read(nurseClient.awaitMessage(topicSub, BUDGET), ReservationEvent.class);
            assertThat(list.type(), is("OFFERS_LIST"));
            assertThat(list.data().toString(), containsString(offer1Id));
            assertThat(list.data().toString(), containsString(offer2Id));

            nurseClient.send("/app/reservation/offer/create",
                    Json.write(Map.of(
                            "serviceRequestId", reservationId.toString(),
                            "proposedPrice", "800.00",
                            "proposedDate", LocalDate.now().plusDays(5).toString(),
                            "proposedTime", "09:00")));
            ReservationEvent offer3 = Json.read(nurseClient.awaitMessage(topicSub, BUDGET), ReservationEvent.class);
            assertThat(offer3.type(), is("OFFER_CREATED"));
            String offer3Id = offerIdOf(offer3);

            patientClient.send("/app/reservation/cancel",
                    Json.write(Map.of("serviceRequestId", reservationId.toString())));
            await().atMost(BUDGET).until(() -> {
                try {
                    return mvc.perform(get("/api/v1/nurse-offers/{id}", offer3Id)
                                    .header("Authorization", bearer(nurseTokens)))
                            .andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString().contains("REJECTED");
                } catch (Exception e) {
                    return false;
                }
            });
            mvc.perform(get("/api/v1/notifications")
                            .header("Authorization", bearer(nurseTokens)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.title == 'Request Cancelled')]", hasSize(1)));

            nurseClient.send("/app/reservation/offer/accept", "not-json");
            SocketErrorPayload conversion = Json.read(nurseClient.awaitMessage(errorsSub, BUDGET),
                    SocketErrorPayload.class);
            assertThat(conversion.code(), is("VALIDATION"));
            assertThat(conversion.message(), containsString("Invalid payload"));
        }
    }

    private String offerIdOf(ReservationEvent event) {
        return Json.read(Json.write(event.data()), iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse.class)
                .id().toString();
    }

    private UUID createRequest(DevOtpAuth.Tokens patient, UUID profileId, UUID serviceTypeId) throws Exception {
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
        return Json.read(body, NearbyServiceRequestResponse.class).serviceRequestId();
    }

    private UUID createAcceptedReservation(DevOtpAuth.Tokens patient, UUID profileId, UUID serviceTypeId,
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

    private String userIdOf(DevOtpAuth.Tokens tokens) throws Exception {
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
        String stamp = "F" + System.nanoTime();
        String body = mvc.perform(multipart("/api/v1/admin/catalog/service-types")
                        .file(image("file"))
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, DevOtpAuth.ADMIN_KEY)
                        .param("name", "WS Flow " + stamp)
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
}
