package iti.jets.java.homenursing.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import iti.jets.java.homenursing.testutil.ApiIntegrationTestBase;
import iti.jets.java.homenursing.testutil.DevOtpAuth;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4: SecurityConfig + JwtAuthenticationFilter probes - the exact public path
 * list, anonymous protection (403 via entry point), invalid/expired bearer rejection
 * (401 via the JWT filter), the admin-key interceptor and the CORS preflight response.
 */
class SecurityConfigIntegrationTest extends ApiIntegrationTestBase {

    @Test
    void publicEndpoints_areReachableAnonymously() throws Exception {
        mvc.perform(get("/api/v1/service-types"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/medical-conditions"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/allergies"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/medications"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoints_requireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/nurses"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidAndExpiredTokens_areRejectedWith401() throws Exception {
        mvc.perform(get("/api/v1/nurses").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/nurses").header("Authorization", "Bearer " + expiredAccessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonBearerHeader_isIgnored_andLeavesRequestAnonymous() throws Exception {
        mvc.perform(get("/api/v1/nurses").header("Authorization", "Basic Zm9vOmJhcg=="))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshTokenPresentedAsAccess_isRejectedWith401() throws Exception {
        mvc.perform(get("/api/v1/nurses").header("Authorization", "Bearer " + tokenWithType("refresh")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoints_requireAdminApiKey() throws Exception {
        mvc.perform(get("/api/v1/admin/nurses"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/nurses")
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, "wrong-key"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/nurses")
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, "   "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflight_isAllowed() throws Exception {
        mvc.perform(options("/api/v1/service-types")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    private String expiredAccessToken() {
        return Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .claim("type", "access")
                .claim("role", "USER")
                .setIssuedAt(new Date(System.currentTimeMillis() - 3_600_000))
                .setExpiration(new Date(System.currentTimeMillis() - 3_000_000))
                .signWith(Keys.hmacShaKeyFor(
                                "test-jwt-secret-for-tests-1234567890".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }

    private String tokenWithType(String type) {
        return Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .claim("type", type)
                .claim("role", "USER")
                .setIssuedAt(new Date(System.currentTimeMillis() - 60_000))
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(
                                "test-jwt-secret-for-tests-1234567890".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }
}