package iti.jets.java.homenursing.dto.auth;

import iti.jets.java.homenursing.dto.nurse.NurseUserResponse;
import iti.jets.java.homenursing.dto.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GoogleAuthResponse {

    public static final String STATUS_AUTHENTICATED = "AUTHENTICATED";
    public static final String STATUS_PHONE_REQUIRED = "PHONE_REQUIRED";

    private final String status;

    private final String pendingToken;

    private final String accessToken;
    private final String refreshToken;
    private final Long expiresIn;
    private final UserResponse user;
    private final NurseUserResponse nurseUser;

    private final String email;
    private final String firstName;
    private final String lastName;
    private final String profileImageUrl;

    public static GoogleAuthResponse authenticated(TokenPair pair) {
        return builder()
                .status(STATUS_AUTHENTICATED)
                .accessToken(pair.getAccessToken())
                .refreshToken(pair.getRefreshToken())
                .expiresIn(pair.getExpiresIn())
                .user(pair.getUser())
                .build();
    }

    public static GoogleAuthResponse authenticated(NurseTokenPair pair) {
        return builder()
                .status(STATUS_AUTHENTICATED)
                .accessToken(pair.getAccessToken())
                .refreshToken(pair.getRefreshToken())
                .expiresIn(pair.getExpiresIn())
                .nurseUser(pair.getUser())
                .build();
    }

    public static GoogleAuthResponse phoneRequired(String pendingToken, String email,
                                                   String firstName, String lastName,
                                                   String profileImageUrl) {
        return builder()
                .status(STATUS_PHONE_REQUIRED)
                .pendingToken(pendingToken)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .profileImageUrl(profileImageUrl)
                .build();
    }
}
