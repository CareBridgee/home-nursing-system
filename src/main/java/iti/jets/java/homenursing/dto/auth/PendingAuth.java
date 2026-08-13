package iti.jets.java.homenursing.dto.auth;

public record PendingAuth(
        String googleSub,
        String email,
        String firstName,
        String lastName,
        String profileImageUrl,
        String role) {
}
