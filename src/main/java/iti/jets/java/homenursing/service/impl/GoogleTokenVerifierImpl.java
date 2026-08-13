package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.exception.UnauthorizedException;
import iti.jets.java.homenursing.service.GoogleTokenVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierImpl.class);

    private final RestClient restClient;
    private final String expectedAudience;

    public GoogleTokenVerifierImpl(RestClient.Builder restClientBuilder,
                                   @Value("${GOOGLE_WEB_CLIENT_ID:}") String expectedAudience) {
        this.restClient = restClientBuilder
                .baseUrl("https://oauth2.googleapis.com")
                .build();
        this.expectedAudience = expectedAudience;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GoogleUserInfo verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException("Invalid Google ID token");
        }
        Map<String, Object> claims;
        try {
            claims = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tokeninfo")
                            .queryParam("id_token", idToken)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.warn("Google token validation failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid Google ID token");
        }
        if (claims == null) {
            throw new UnauthorizedException("Invalid Google ID token");
        }

        if (expectedAudience.isBlank() || !expectedAudience.equals(claims.get("aud"))) {
            throw new UnauthorizedException("Google ID token was not issued for this application");
        }

        Object emailVerified = claims.get("email_verified");
        boolean verified = Boolean.TRUE.equals(emailVerified)
                || "true".equalsIgnoreCase(String.valueOf(emailVerified));
        String email = (String) claims.get("email");
        if (!verified || email == null || email.isBlank()) {
            throw new UnauthorizedException("Google email is not verified");
        }

        String sub = (String) claims.get("sub");
        if (sub == null || sub.isBlank()) {
            throw new UnauthorizedException("Google ID token is missing the subject claim");
        }

        return new GoogleUserInfo(
                sub,
                email,
                (String) claims.get("given_name"),
                (String) claims.get("family_name"),
                (String) claims.get("picture"));
    }
}
