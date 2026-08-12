package iti.jets.java.homenursing.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUserId_withoutAuthentication_throws() {
        SecurityContextHolder.clearContext();
        assertThrows(IllegalStateException.class, SecurityUtils::currentUserId);
    }

    @Test
    void currentUserId_withNullPrincipal_throws() {
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        assertThrows(IllegalStateException.class, SecurityUtils::currentUserId);
    }

    @Test
    void currentUserId_returnsPrincipalAsUuid() {
        UUID id = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(id, null));
        assertThat(SecurityUtils.currentUserId(), is(id));
    }
}