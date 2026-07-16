package iti.jets.java.carenest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenPair {
    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;
    private final UserResponse user;
}
