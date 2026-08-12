package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.nurse.NurseResponse;
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
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 3: WebSocket presence suite - heartbeat/availability/location updates through the
 * real STOMP stack (SimpleBroker + Redis-backed {@link WebSocketPresenceService}), plus the
 * error-queue payload shapes (FORBIDDEN for unapproved nurses, VALIDATION for bad payloads).
 */
class WebSocketPresenceIntegrationTest extends BaseWebSocketIntegrationTest {

    private static final Duration BUDGET = Duration.ofSeconds(10);

    @Autowired
    private WebSocketPresenceService presenceService;

    @Test
    void presence_lifecycle_heartbeat_availability_location_disconnect() throws Exception {
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111510001");
        String nurseId = registerAndApprove(nurseTokens, "+201111510001");
        String nurseUserId = userIdOf(nurseTokens);

        try (StompTestClient nurse = new StompTestClient(wsUrl(), bearer(nurseTokens)).connect(BUDGET)) {
            nurse.subscribe("/user/queue/errors");

            nurse.send("/app/heartbeat", "{}");
            await().atMost(BUDGET).until(() -> presenceService.getOnlineNurses().contains(nurseUserId));
            assertTrue(nurse.errors().isEmpty(), "nurse heartbeat must not error: " + nurse.errors());

            nurse.send("/app/reservation/availability",
                    Json.write(Map.of("available", true, "lat", "30.0444", "lng", "31.2357")));
            await().atMost(BUDGET).until(() -> presenceService.getAvailableLocation(nurseUserId).isPresent());
            assertThat(presenceService.getAvailableLocation(nurseUserId).orElseThrow().getY(),
                    closeTo(30.0444, 0.001));

            nurse.send("/app/reservation/location",
                    Json.write(Map.of("lat", "30.05", "lng", "31.24")));
            await().atMost(BUDGET).until(() -> presenceService.getAvailableLocation(nurseUserId)
                    .map(p -> Math.abs(p.getX() - 31.24) < 0.001)
                    .orElse(false));

            nurse.send("/app/reservation/availability",
                    Json.write(Map.of("available", false)));
            await().atMost(BUDGET).until(() -> presenceService.getAvailableLocation(nurseUserId).isEmpty());

            nurse.disconnect();
            await().atMost(BUDGET).until(() -> !presenceService.getOnlineNurses().contains(nurseUserId));
        }
    }

    @Test
    void presence_requiresApprovedNurse_andValidationPayloads() throws Exception {
        DevOtpAuth.Tokens unapprovedTokens = DevOtpAuth.loginNurse(mvc, "+201111510002");
        registerNurseOnly(unapprovedTokens, "+201111510002");
        String unapprovedUserId = userIdOf(unapprovedTokens);

        try (StompTestClient unapproved =
                     new StompTestClient(wsUrl(), bearer(unapprovedTokens)).connect(BUDGET)) {
            String errorsSub = unapproved.subscribe("/user/queue/errors");

            unapproved.send("/app/heartbeat", "{}");
            String errorPayload = unapproved.awaitMessage(errorsSub, BUDGET);
            SocketErrorPayload payload = Json.read(errorPayload, SocketErrorPayload.class);
            assertThat(payload.code(), is("FORBIDDEN"));
            assertThat(payload.message(), containsString("approved"));
        }

        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111510003");
        registerAndApprove(nurseTokens, "+201111510003");
        String nurseUserId = userIdOf(nurseTokens);

        try (StompTestClient nurse = new StompTestClient(wsUrl(), bearer(nurseTokens)).connect(BUDGET)) {
            String errorsSub = nurse.subscribe("/user/queue/errors");

            nurse.send("/app/reservation/availability",
                    Json.write(Map.of("available", true)));
            String errorPayload = nurse.awaitMessage(errorsSub, BUDGET);
            SocketErrorPayload payload = Json.read(errorPayload, SocketErrorPayload.class);
            assertThat(payload.code(), is("VALIDATION"));
            assertThat(payload.message(), containsString("lat and lng are required"));
        }
    }

    private String registerNurseOnly(DevOtpAuth.Tokens nurseTokens, String phone) throws Exception {
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
        return Json.read(body, NurseResponse.class).getId().toString();
    }

    private String registerAndApprove(DevOtpAuth.Tokens nurseTokens, String phone) throws Exception {
        String nurseId = registerNurseOnly(nurseTokens, phone);
        mvc.perform(patch("/api/v1/admin/nurses/{nurseId}/approve", nurseId)
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, DevOtpAuth.ADMIN_KEY))
                .andExpect(status().isOk());
        return nurseId;
    }

    private String userIdOf(DevOtpAuth.Tokens tokens) throws Exception {
        String body = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/users/me")
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, iti.jets.java.homenursing.dto.user.UserResponse.class).getId().toString();
    }
}
