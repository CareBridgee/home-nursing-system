package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.exception.UnauthorizedException;
import iti.jets.java.homenursing.service.impl.GoogleTokenVerifierImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("unit")
class GoogleTokenVerifierImplTest {

    private static final String CLIENT_ID = "web-client-123.apps.googleusercontent.com";

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private GoogleTokenVerifierImpl verifier;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("https://oauth2.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        verifier = new GoogleTokenVerifierImpl(builder, CLIENT_ID);
    }

    @Test
    void verifyAcceptsValidTokenWithMatchingAudienceAndVerifiedEmail() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("https://oauth2.googleapis.com/tokeninfo")))
                .andExpect(queryParam("id_token", "google-token"))
                .andRespond(withSuccess(
                        """
                        {"aud":"%s","sub":"sub-1","email":"jane@example.com",
                         "email_verified":true,"given_name":"Jane","family_name":"Doe","picture":"https://pic"}
                        """.formatted(CLIENT_ID), MediaType.APPLICATION_JSON));

        GoogleTokenVerifier.GoogleUserInfo info = verifier.verify("google-token");

        assertThat(info.googleSub()).isEqualTo("sub-1");
        assertThat(info.email()).isEqualTo("jane@example.com");
        assertThat(info.givenName()).isEqualTo("Jane");
        assertThat(info.familyName()).isEqualTo("Doe");
        assertThat(info.picture()).isEqualTo("https://pic");
    }

    @Test
    void verifyRejectsTokenWithWrongAudience() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("https://oauth2.googleapis.com/tokeninfo")))
                .andRespond(withSuccess(
                        """
                        {"aud":"some-other-client","sub":"sub-1","email":"jane@example.com","email_verified":true}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("google-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not issued for this application");
    }

    @Test
    void verifyRejectsUnverifiedEmail() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("https://oauth2.googleapis.com/tokeninfo")))
                .andRespond(withSuccess(
                        """
                        {"aud":"%s","sub":"sub-1","email":"jane@example.com","email_verified":false}
                        """.formatted(CLIENT_ID), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("google-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("email is not verified");
    }

    @Test
    void verifyRejectsMissingEmail() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("https://oauth2.googleapis.com/tokeninfo")))
                .andRespond(withSuccess(
                        """
                        {"aud":"%s","sub":"sub-1","email_verified":true}
                        """.formatted(CLIENT_ID), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("google-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("email is not verified");
    }

    @Test
    void verifyRejectsTokenInfoHttpError() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("https://oauth2.googleapis.com/tokeninfo")))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> verifier.verify("expired-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid Google ID token");
    }

    @Test
    void verifyRejectsBlankIdTokenWithoutCallingGoogle() {
        assertThatThrownBy(() -> verifier.verify("  "))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verifyRejectsEveryTokenWhenClientIdNotConfigured() {
        RestClient.Builder blankBuilder = RestClient.builder().baseUrl("https://oauth2.googleapis.com");
        MockRestServiceServer blankServer = MockRestServiceServer.bindTo(blankBuilder).build();
        GoogleTokenVerifierImpl unconfigured = new GoogleTokenVerifierImpl(blankBuilder, "");
        blankServer.expect(requestTo(org.hamcrest.Matchers.containsString("https://oauth2.googleapis.com/tokeninfo")))
                .andRespond(withSuccess(
                        """
                        {"aud":"whatever","sub":"sub-1","email":"jane@example.com","email_verified":true}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> unconfigured.verify("google-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not issued for this application");
    }

    @Test
    void verifyExtractsFieldsFromPlainJsonObject() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("https://oauth2.googleapis.com/tokeninfo")))
                .andRespond(withSuccess(
                        """
                        {"aud":"%s","sub":"sub-9","email":"x@example.com","email_verified":true,
                         "given_name":"X","family_name":"Y"}
                        """.formatted(CLIENT_ID), MediaType.APPLICATION_JSON));

        GoogleTokenVerifier.GoogleUserInfo info = verifier.verify("google-token");

        assertThat(info.googleSub()).isEqualTo("sub-9");
        assertThat(info.email()).isEqualTo("x@example.com");
        assertThat(info.givenName()).isEqualTo("X");
    }
}
