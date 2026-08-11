package iti.jets.java.homenursing.testutil;

import iti.jets.java.homenursing.dto.auth.DevOtpResponse;
import iti.jets.java.homenursing.dto.auth.TokenPair;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dev-OTP login helpers for integration tests. Mirrors the .mjs auth flow:
 * request-otp (dev) returns the OTP in the response body, then verify-otp returns tokens.
 */
public final class DevOtpAuth {

    public static final String ADMIN_KEY = "test-admin-key";

    private DevOtpAuth() {
    }

    public record Tokens(String accessToken, String refreshToken, long expiresIn) {
    }

    /** Logs in (auto-registering) as a regular user; returns the token pair. */
    public static Tokens loginPatient(MockMvc mvc, String phone) throws Exception {
        String otp = requestOtp(mvc, "/api/v1/auth/dev/request-otp", phone);
        String body = mvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone, "otp", otp))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return toTokens(Json.read(body, TokenPair.class));
    }

    /** Logs in (auto-registering) as a nurse; returns the token pair. */
    public static Tokens loginNurse(MockMvc mvc, String phone) throws Exception {
        String otp = requestOtp(mvc, "/api/v1/auth/dev/request-otp", phone);
        String body = mvc.perform(post("/api/v1/auth/nurse/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone, "otp", otp))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var pair = Json.read(body, iti.jets.java.homenursing.dto.auth.NurseTokenPair.class);
        return new Tokens(pair.getAccessToken(), pair.getRefreshToken(), pair.getExpiresIn());
    }

    private static String requestOtp(MockMvc mvc, String path, String phone) throws Exception {
        String body = mvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, DevOtpResponse.class).otp();
    }

    private static Tokens toTokens(TokenPair pair) {
        return new Tokens(pair.getAccessToken(), pair.getRefreshToken(), pair.getExpiresIn());
    }
}
