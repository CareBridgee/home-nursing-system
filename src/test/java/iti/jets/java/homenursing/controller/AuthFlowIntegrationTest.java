package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.testutil.ApiIntegrationTestBase;
import iti.jets.java.homenursing.testutil.DevOtpAuth;
import iti.jets.java.homenursing.testutil.Json;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends ApiIntegrationTestBase {

    @Test
    void devOtp_verify_refresh_logout_fullCycle() throws Exception {
        String phone = "+201111100001";
        String otp = mvc.perform(post("/api/v1/auth/dev/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otp", not(blankString())))
                .andReturn().getResponse().getContentAsString();
        String code = Json.read(otp, iti.jets.java.homenursing.dto.auth.DevOtpResponse.class).otp();

        String body = mvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone, "otp", code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(blankString())))
                .andExpect(jsonPath("$.refreshToken", not(blankString())))
                .andReturn().getResponse().getContentAsString();
        DevOtpAuth.ApiToken pair = Json.read(body, DevOtpAuth.ApiToken.class);

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("refreshToken", pair.refreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(blankString())));

        mvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("refreshToken", pair.refreshToken()))))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("refreshToken", pair.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongOtp_isRejected() throws Exception {
        String phone = "+201111100002";
        mvc.perform(post("/api/v1/auth/dev/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone, "otp", "000000"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationErrors_onBadLoginPayloads() throws Exception {
        mvc.perform(post("/api/v1/auth/dev/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", "+201111100003"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendOtpEndpoints_areNoContent() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", "+201111100004"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/nurse/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", "+201111100004"))))
                .andExpect(status().isOk());
    }

    @Test
    void getProfileByPhone() throws Exception {
        String phone = "+201111100005";
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, phone);
        mvc.perform(get("/api/v1/auth/profile").param("phoneNumber", phone)
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber", is(phone)));
        mvc.perform(get("/api/v1/auth/profile").param("phoneNumber", "+201111199999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void nurseVerifyFlow_returnsNurseTokenPair() throws Exception {
        String phone = "+201111100006";
        String otp = Json.read(mvc.perform(post("/api/v1/auth/dev/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
                iti.jets.java.homenursing.dto.auth.DevOtpResponse.class).otp();
        mvc.perform(post("/api/v1/auth/nurse/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone, "otp", otp))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(blankString())));

        String otp2 = Json.read(mvc.perform(post("/api/v1/auth/dev/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
                iti.jets.java.homenursing.dto.auth.DevOtpResponse.class).otp();
        mvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone, "otp", otp2))))
                .andExpect(status().isConflict());
    }
}