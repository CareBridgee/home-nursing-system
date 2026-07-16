package iti.jets.java.carenest.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import iti.jets.java.carenest.exception.UnauthorizedException;
import iti.jets.java.carenest.service.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
public class TokenServiceImpl implements TokenService {

    @Value("${JWT_SECRET:secretKeyForJwtSigningAndValidation1234567890AB}")
    private String jwtSecret;

    @Value("${JWT_ACCESS_TOKEN_TTL_MIN:15}")
    private long accessTokenTtlMinutes;

    @Value("${JWT_REFRESH_TOKEN_TTL_DAYS:30}")
    private long refreshTokenTtlDays;

    private final StringRedisTemplate redisTemplate;

    public TokenServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlMinutes * 60;
    }

    @Override
    public String generateAccessToken(String userId) {
        long now = System.currentTimeMillis();
        long expiration = now + (accessTokenTtlMinutes * 60 * 1000);

        return Jwts.builder()
                .setSubject(userId)
                .claim("type", "access")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String generateRefreshToken(String userId) {
        String refreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                "refresh:" + refreshToken,
                userId,
                Duration.ofDays(refreshTokenTtlDays));

        return refreshToken;
    }

    @Override
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            getUserIdFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isAccessToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return "access".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getUserIdFromRefreshToken(String token) {
        String userId = redisTemplate.opsForValue().get("refresh:" + token);
        if (userId == null) {
            throw new UnauthorizedException("Refresh token not found or expired");
        }
        return userId;
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        redisTemplate.delete("refresh:" + refreshToken);
    }

    @Override
    public void validateRefreshToken(String refreshToken) {
        String storedUserId = redisTemplate.opsForValue().get("refresh:" + refreshToken);
        if (storedUserId == null) {
            throw new UnauthorizedException("Refresh token not found or expired");
        }
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public void expire(String key, Duration ttl) {
        redisTemplate.expire(key, ttl);
    }
}
