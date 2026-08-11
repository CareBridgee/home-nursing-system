package iti.jets.java.homenursing.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import iti.jets.java.homenursing.exception.UnauthorizedException;
import iti.jets.java.homenursing.service.impl.TokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    private static final String JWT_SECRET = "secretKeyForJwtSigningAndValidation1234567890AB";
    private static final long ACCESS_TTL_MINUTES = 15;
    private static final long REFRESH_TTL_DAYS = 30;

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenServiceImpl(redisTemplate);
        ReflectionTestUtils.setField(tokenService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(tokenService, "accessTokenTtlMinutes", ACCESS_TTL_MINUTES);
        ReflectionTestUtils.setField(tokenService, "refreshTokenTtlDays", REFRESH_TTL_DAYS);
    }

    private static SecretKey signingKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private static Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey()).build().parseClaimsJws(token).getBody();
    }

    private static String tokenWithType(String subject, String type) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .claim("type", type)
                .claim("role", "USER")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 60_000))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void getAccessTokenTtlSecondsMultipliesMinutesBySixty() {
        assertThat(tokenService.getAccessTokenTtlSeconds()).isEqualTo(900);
    }

    @Test
    void generateAccessTokenCarriesSubjectTypeRoleAndFifteenMinuteExpiry() {
        String token = tokenService.generateAccessToken("user-1", "NURSE");

        Claims claims = parse(token);
        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.get("role", String.class)).isEqualTo("NURSE");
        long lifetimeMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(lifetimeMillis).isEqualTo(ACCESS_TTL_MINUTES * 60 * 1000);
    }

    @Test
    void getRoleFromTokenReturnsRoleClaim() {
        String token = tokenService.generateAccessToken("user-1", "ADMIN");

        assertThat(tokenService.getRoleFromToken(token)).isEqualTo("ADMIN");
    }

    @Test
    void getUserIdFromTokenReturnsSubject() {
        String token = tokenService.generateAccessToken("user-42", "USER");

        assertThat(tokenService.getUserIdFromToken(token)).isEqualTo("user-42");
    }

    @Test
    void isTokenValidAcceptsWellFormedToken() {
        String token = tokenService.generateAccessToken("user-1", "USER");

        assertThat(tokenService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValidRejectsGarbageToken() {
        assertThat(tokenService.isTokenValid("not-a-jwt")).isFalse();
    }

    @Test
    void isAccessTokenTrueForAccessToken() {
        String token = tokenService.generateAccessToken("user-1", "USER");

        assertThat(tokenService.isAccessToken(token)).isTrue();
    }

    @Test
    void isAccessTokenFalseForOtherTokenType() {
        String token = tokenWithType("user-1", "refresh");

        assertThat(tokenService.isAccessToken(token)).isFalse();
    }

    @Test
    void isAccessTokenFalseForGarbageToken() {
        assertThat(tokenService.isAccessToken("garbage")).isFalse();
    }

    @Test
    void generateRefreshTokenStoresUserIdWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = tokenService.generateRefreshToken("user-1");

        assertThat(token).isNotBlank();
        verify(valueOperations).set(
                eq("refresh:" + token), eq("user-1"), eq(Duration.ofDays(REFRESH_TTL_DAYS)));
    }

    @Test
    void getUserIdFromRefreshTokenReturnsStoredUserId() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:token-1")).thenReturn("user-1");

        assertThat(tokenService.getUserIdFromRefreshToken("token-1")).isEqualTo("user-1");
    }

    @Test
    void getUserIdFromRefreshTokenThrowsForUnknownToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertThatThrownBy(() -> tokenService.getUserIdFromRefreshToken("missing"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Refresh token not found or expired");
    }

    @Test
    void revokeRefreshTokenDeletesKey() {
        tokenService.revokeRefreshToken("token-1");

        verify(redisTemplate).delete("refresh:token-1");
    }

    @Test
    void validateRefreshTokenPassesWhenPresent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:token-1")).thenReturn("user-1");

        tokenService.validateRefreshToken("token-1");
    }

    @Test
    void validateRefreshTokenThrowsWhenMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertThatThrownBy(() -> tokenService.validateRefreshToken("missing"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Refresh token not found or expired");
    }

    @Test
    void setStoresValueWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenService.set("key-1", "value-1", Duration.ofSeconds(5));

        verify(valueOperations).set("key-1", "value-1", Duration.ofSeconds(5));
    }

    @Test
    void getReturnsStoredValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key-1")).thenReturn("value-1");

        assertThat(tokenService.get("key-1")).isEqualTo("value-1");
    }

    @Test
    void deleteDelegatesToRedis() {
        tokenService.delete("key-1");

        verify(redisTemplate).delete("key-1");
    }

    @Test
    void incrementDelegatesToRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("counter")).thenReturn(5L);

        assertThat(tokenService.increment("counter")).isEqualTo(5L);
    }

    @Test
    void expireDelegatesToRedis() {
        tokenService.expire("key-1", Duration.ofSeconds(10));

        verify(redisTemplate).expire("key-1", Duration.ofSeconds(10));
    }
}
