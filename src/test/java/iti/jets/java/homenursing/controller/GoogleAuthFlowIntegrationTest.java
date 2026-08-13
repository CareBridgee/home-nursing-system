package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.exception.UnauthorizedException;
import iti.jets.java.homenursing.service.GoogleTokenVerifier;
import iti.jets.java.homenursing.testutil.ApiIntegrationTestBase;
import iti.jets.java.homenursing.testutil.Json;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoogleAuthFlowIntegrationTest extends ApiIntegrationTestBase {

    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

    private void stubValidGoogleToken(String sub, String email) {
        when(googleTokenVerifier.verify(anyString()))
                .thenReturn(new GoogleTokenVerifier.GoogleUserInfo(
                        sub, email, "Jane", "Doe", "https://pic.example/jane.jpg"));
    }

    private String requestGoogleLogin(String path) throws Exception {
        return mvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("idToken", "google-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is("PHONE_REQUIRED")))
                .andExpect(jsonPath("$.pendingToken", not(blankOrNullString())))
                .andReturn().getResponse().getContentAsString();
    }

    private String completePhoneStep(String phone, String pendingToken) throws Exception {
        String otp = Json.read(mvc.perform(post("/api/v1/auth/dev/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
                iti.jets.java.homenursing.dto.auth.DevOtpResponse.class).otp();

        return mvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "phoneNumber", phone,
                                "otp", otp,
                                "pendingToken", pendingToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is("AUTHENTICATED")))
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken", not(blankOrNullString())))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void googleLogin_withoutPhone_requiresPhoneThenLinksAndAuthenticates() throws Exception {
        stubValidGoogleToken("google-sub-a", "a@example.com");
        String phone = "+201111100101";

        String loginBody = requestGoogleLogin("/api/v1/auth/google");
        String pendingToken = Json.read(loginBody,
                com.fasterxml.jackson.databind.JsonNode.class).get("pendingToken").asText();

        String body = completePhoneStep(phone, pendingToken);
        com.fasterxml.jackson.databind.JsonNode node =
                Json.read(body, com.fasterxml.jackson.databind.JsonNode.class);
        org.assertj.core.api.Assertions.assertThat(node.get("user").get("phoneNumber").asText())
                .isEqualTo(phone);
        org.assertj.core.api.Assertions.assertThat(node.get("user").get("email").asText())
                .isEqualTo("a@example.com");
        org.assertj.core.api.Assertions.assertThat(node.get("user").get("firstName").asText())
                .isEqualTo("Jane");

        mvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("idToken", "google-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is("AUTHENTICATED")))
                .andExpect(jsonPath("$.user.phoneNumber", org.hamcrest.Matchers.is(phone)));
    }

    @Test
    void nurseGoogleLogin_createsNurseRecordAndCompletesWithPhone() throws Exception {
        stubValidGoogleToken("google-sub-b", "b@example.com");
        String phone = "+201111100102";

        String loginBody = requestGoogleLogin("/api/v1/auth/nurse/google");
        String pendingToken = Json.read(loginBody,
                com.fasterxml.jackson.databind.JsonNode.class).get("pendingToken").asText();

        mvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "phoneNumber", phone,
                                "otp", Json.read(mvc.perform(post("/api/v1/auth/dev/request-otp")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(Json.write(Map.of("phoneNumber", phone))))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString(),
                                iti.jets.java.homenursing.dto.auth.DevOtpResponse.class).otp(),
                                "pendingToken", pendingToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is("AUTHENTICATED")))
                .andExpect(jsonPath("$.nurseUser", not(org.hamcrest.Matchers.nullValue())))
                .andExpect(jsonPath("$.nurseUser.nurse.verificationStatus",
                        org.hamcrest.Matchers.is("UNDER_REVIEW")));
    }

    @Test
    void crossRoleGoogleLogin_conflicts() throws Exception {
        stubValidGoogleToken("google-sub-c", "c@example.com");
        String phone = "+201111100103";

        String loginBody = requestGoogleLogin("/api/v1/auth/google");
        String pendingToken = Json.read(loginBody,
                com.fasterxml.jackson.databind.JsonNode.class).get("pendingToken").asText();
        completePhoneStep(phone, pendingToken);

        mvc.perform(post("/api/v1/auth/nurse/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("idToken", "google-token"))))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidGoogleToken_isUnauthorized() throws Exception {
        when(googleTokenVerifier.verify(anyString()))
                .thenThrow(new UnauthorizedException("Invalid Google ID token"));

        mvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("idToken", "bad-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleLogin_withMissingIdToken_isBadRequest() throws Exception {
        mvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
