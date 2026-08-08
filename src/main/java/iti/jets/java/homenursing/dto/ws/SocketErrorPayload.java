package iti.jets.java.homenursing.dto.ws;

import java.time.Instant;

public record SocketErrorPayload(
        String code,
        String message,
        Instant timestamp
) {
    public static SocketErrorPayload of(String code, String message) {
        return new SocketErrorPayload(code, message, Instant.now());
    }
}