package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NurseTokenPair {
    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;
    private final NurseResponse nurse;
}