package iti.jets.java.homenursing.controller;

import com.google.genai.errors.ClientException;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.service.ChatDraftService;
import iti.jets.java.homenursing.testutil.ApiIntegrationTestBase;
import iti.jets.java.homenursing.testutil.DevOtpAuth;
import iti.jets.java.homenursing.testutil.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 3: chat endpoints - the AI ChatClient bean is mocked (see BaseIntegrationTest);
 * ChatMessageController is exercised against a real reservation with participants.
 */
class ChatFlowIntegrationTest extends ApiIntegrationTestBase {

    private static final String LAT = "30.0444";
    private static final String LNG = "31.2357";

    @Autowired
    private ChatDraftService chatDraftService;

    @Autowired
    private ChatController chatController;

    private ChatClient.ChatClientRequestSpec chatSpec;
    private ChatClient.CallResponseSpec callSpec;
    private ChatClient.StreamResponseSpec streamSpec;

    @BeforeEach
    void setUpChatMocks() {
        chatSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);
        streamSpec = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(chatSpec);
        when(chatSpec.advisors(org.mockito.ArgumentMatchers.<java.util.function.Consumer<ChatClient.AdvisorSpec>>any()))
                .thenReturn(chatSpec);
        when(chatSpec.toolContext(any())).thenReturn(chatSpec);
        when(chatSpec.system(anyString())).thenReturn(chatSpec);
        when(chatSpec.user(anyString())).thenReturn(chatSpec);
        when(chatSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Sure, I can help with that.");
        when(chatSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just("Hello from AI"));
    }

    private UUID seedServiceType() throws Exception {
        String stamp = "C" + System.nanoTime();
        String body = mvc.perform(multipart("/api/v1/admin/catalog/service-types")
                        .file(image("file"))
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, DevOtpAuth.ADMIN_KEY)
                        .param("name", "Chat Flow " + stamp)
                        .param("description", "basic")
                        .param("category", "NURSING")
                        .param("estimatedDurationMinutes", "90")
                        .param("basePrice", "400.00"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, iti.jets.java.homenursing.dto.catalog.ServiceTypeResponse.class).id();
    }

    private UUID defaultProfileId(DevOtpAuth.Tokens tokens) throws Exception {
        String body = mvc.perform(get("/api/v1/profiles/default").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, iti.jets.java.homenursing.dto.profile.ProfileResponse.class).getId();
    }

    private String currentUserId(DevOtpAuth.Tokens tokens) throws Exception {
        String body = mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, iti.jets.java.homenursing.dto.user.UserResponse.class).getId().toString();
    }

    private String registerNurse(DevOtpAuth.Tokens nurseTokens, String nationalId) throws Exception {
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

    private record NurseAndId(String nurseId, UUID serviceTypeId) {
    }

    private NurseAndId registeredNurse(DevOtpAuth.Tokens nurseTokens, String phone) throws Exception {
        UUID serviceTypeId = seedServiceType();
        String nurseId = registerNurse(nurseTokens, "30000000" + phone.substring(phone.length() - 6));
        mvc.perform(patch("/api/v1/admin/nurses/{nurseId}/approve", nurseId)
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, DevOtpAuth.ADMIN_KEY))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/nurses/{nurseId}/services", nurseId)
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(java.util.List.of(Map.of("serviceTypeId", serviceTypeId.toString())))))
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

    private void stubReply(String reply) {
        when(callSpec.content()).thenReturn(reply);
    }

    @Test
    void chat_textInput_confirm_reset_flows() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111400001");
        UUID profileId = defaultProfileId(patient);

        stubReply("How can I help you today?");
        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("profileId", profileId.toString(), "message", "hello"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageType", is("INPUT")))
                .andExpect(jsonPath("$.reply", is("How can I help you today?")))
                .andExpect(jsonPath("$.draft.complete", is(false)))
                .andExpect(jsonPath("$.urgency", nullValue()));

        stubReply("Sure, I can help with that.");
        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("profileId", profileId.toString(), "message", "hi"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageType", is("TEXT")));

        UUID serviceTypeId = seedServiceType();
        chatDraftService.updateField(profileId, "serviceTypeId", serviceTypeId.toString());
        stubReply("Your request is ready for confirmation.");
        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("profileId", profileId.toString(), "message", "book"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageType", is("CONFIRM")))
                .andExpect(jsonPath("$.draft.complete", is(true)))
                .andExpect(jsonPath("$.draft.serviceTypeId", is(serviceTypeId.toString())));

        mvc.perform(post("/api/v1/chat/reset")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("profileId", profileId.toString()))))
                .andExpect(status().isNoContent());

        stubReply("Sure, I can help with that.");
        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("profileId", profileId.toString(), "message", "again"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageType", is("TEXT")));
    }

    @Test
    void chat_urgency_signal_andValidationErrors() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111400002");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens other = DevOtpAuth.loginPatient(mvc, "+201111400003");
        UUID otherProfileId = defaultProfileId(other);

        chatDraftService.setUrgency(profileId, true, "HOSPITALIZATION", "chest pain");
        stubReply("Please seek emergency care.");
        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("profileId", profileId.toString(), "message", "chest pain"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageType", is("URGENT")))
                .andExpect(jsonPath("$.urgency.level", is("HOSPITALIZATION")))
                .andExpect(jsonPath("$.urgency.advice", containsString("123")));

        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("profileId", profileId.toString(), "message", "  "))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("message", "x".repeat(2001)))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("profileId", profileId.toString()))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("profileId", otherProfileId.toString(), "message", "hi"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void chat_aiFailure_handlers_andRetry() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111400004");
        UUID profileId = defaultProfileId(patient);
        String chatBody = Json.write(Map.of("profileId", profileId.toString(), "message", "hi"));

        ClientException quotaExceeded = new ClientException(429, "RESOURCE_EXHAUSTED", "quota exceeded");
        doThrow(quotaExceeded).when(callSpec).content();
        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error", is("AI_SERVICE_UNAVAILABLE")));

        ClientException toolRejected = new ClientException(400, "INVALID_ARGUMENT", "bad tool call");
        doThrow(toolRejected).when(callSpec).content();
        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody))
                .andExpect(status().isBadRequest());

        doThrow(new IllegalStateException("provider down")).when(callSpec).content();
        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("AI_SERVICE_UNAVAILABLE")));

        doThrow(new IllegalStateException("transient"))
                .doReturn("Recovered answer")
                .when(callSpec).content();
        mvc.perform(post("/api/v1/chat")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", is("Recovered answer")));
    }

    @Test
    void chat_stream_successAndFailureFallback() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111400005");
        UUID profileId = defaultProfileId(patient);
        String patientUserId = currentUserId(patient);
        String chatBody = Json.write(Map.of("profileId", profileId.toString(), "message", "stream"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(patientUserId, null, List.of()));
        try {
            Flux<String> okFlux = chatController.chatStream(
                    new ChatController.ChatRequest(profileId, "stream"));
            assertThat(okFlux.collectList().block(), hasItem(containsString("Hello from AI")));

            when(streamSpec.content()).thenReturn(Flux.error(new IllegalStateException("stream boom")));
            Flux<String> failedFlux = chatController.chatStream(
                    new ChatController.ChatRequest(profileId, "stream"));
            assertThat(failedFlux.collectList().block(), hasItem(containsString("AI_SERVICE_UNAVAILABLE")));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void chatMessages_participantRules_afterFilter_andNotifications() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111400006");
        UUID profileId = defaultProfileId(patient);
        String patientUserId = currentUserId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111400007");
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111400007");
        String nurseUserId = currentUserId(nurseTokens);
        DevOtpAuth.Tokens stranger = DevOtpAuth.loginPatient(mvc, "+201111400008");

        UUID reservationId = createReservation(patient, profileId, nurse.serviceTypeId(), nurseTokens);

        mvc.perform(post("/api/v1/reservations/{id}/messages", reservationId)
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("content", "Hello nurse"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", is("Hello nurse")))
                .andExpect(jsonPath("$.senderUserId", is(patientUserId)))
                .andExpect(jsonPath("$.serviceRequestId", is(reservationId.toString())));

        mvc.perform(post("/api/v1/reservations/{id}/messages", reservationId)
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("content", "Hello patient"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderUserId", is(nurseUserId)));

        mvc.perform(get("/api/v1/reservations/{id}/messages", reservationId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content", is("Hello nurse")))
                .andExpect(jsonPath("$[1].content", is("Hello patient")));

        String oneHourAgo = LocalDateTime.now().minusHours(1).toString();
        String oneHourAhead = LocalDateTime.now().plusHours(1).toString();
        mvc.perform(get("/api/v1/reservations/{id}/messages", reservationId)
                        .param("after", oneHourAgo)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/v1/reservations/{id}/messages", reservationId)
                        .param("after", oneHourAhead)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));

        mvc.perform(post("/api/v1/reservations/{id}/messages", reservationId)
                        .header("Authorization", bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("content", "intrusion"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reservations/{id}/messages", reservationId)
                        .header("Authorization", bearer(stranger)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/reservations/{id}/messages", reservationId)
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("content", ""))))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'New Message')]", hasSize(1)));
        mvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'New Message')]", hasSize(1)));
    }
}
