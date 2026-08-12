package iti.jets.java.homenursing.testutil;

import com.fasterxml.jackson.databind.JsonNode;
import iti.jets.java.homenursing.dto.auth.DevOtpResponse;
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

    public static final String ADMIN_KEY_HEADER = "X-Admin-API-Key";
    public static final String ADMIN_KEY = "test-admin-key";

    private DevOtpAuth() {
    }

    /** Test-side mirror of the auth TokenPair response (TokenPair itself has no JSON ctor). */
    public record ApiToken(String accessToken, String refreshToken, long expiresIn, JsonNode user) {
    }

    public record Tokens(String accessToken, String refreshToken, long expiresIn) {
    }

    /** Logs in (auto-registering) as a regular user; returns the token pair. */
    public static Tokens loginPatient(MockMvc mvc, String phone) throws Exception {
        ApiToken api = verify(mvc, "/api/v1/auth/verify-otp", phone);
        return new Tokens(api.accessToken(), api.refreshToken(), api.expiresIn());
    }

    /** Logs in (auto-registering) as a nurse; returns the token pair. */
    public static Tokens loginNurse(MockMvc mvc, String phone) throws Exception {
        ApiToken api = verify(mvc, "/api/v1/auth/nurse/verify-otp", phone);
        return new Tokens(api.accessToken(), api.refreshToken(), api.expiresIn());
    }

    private static ApiToken verify(MockMvc mvc, String path, String phone) throws Exception {
        String otp = requestOtp(mvc, "/api/v1/auth/dev/request-otp", phone);
        String body = mvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone, "otp", otp))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, ApiToken.class);
    }

    private static String requestOtp(MockMvc mvc, String path, String phone) throws Exception {
        String body = mvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("phoneNumber", phone))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, DevOtpResponse.class).otp();
    }
}
