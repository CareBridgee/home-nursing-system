package iti.jets.java.homenursing.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleLoginRequest {
    @NotBlank(message = "Google ID token is required")
    private String idToken;
}
