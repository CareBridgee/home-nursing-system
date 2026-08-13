package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.auth.PendingAuth;

import java.time.Duration;

public interface TokenService {

    String generateAccessToken(String userId, String role);

    long getAccessTokenTtlSeconds();

    String generateRefreshToken(String userId);

    String getUserIdFromToken(String token);

    String getRoleFromToken(String token);

    boolean isTokenValid(String token);

    boolean isAccessToken(String token);

    String getUserIdFromRefreshToken(String token);

    void revokeRefreshToken(String refreshToken);

    void validateRefreshToken(String refreshToken);

    String generatePendingToken(PendingAuth pendingAuth);

    PendingAuth parsePendingToken(String token);

    void set(String key, String value, Duration ttl);

    String get(String key);

    void delete(String key);

    Long increment(String key);

    void expire(String key, Duration ttl);
}
