package iti.jets.java.homenursing.dto.auth;

import iti.jets.java.homenursing.dto.user.UserResponse;
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
