package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.TokenPair;
import iti.jets.java.homenursing.dto.UserResponse;

public interface AuthService {

    void requestOtp(String phoneNumber);

    TokenPair verifyOtpAndLogin(String phoneNumber, String otp);

    TokenPair verifyNurseOtpAndLogin(String phoneNumber, String otp);

    TokenPair refreshToken(String refreshToken);

    void logout(String refreshToken);

    UserResponse getUserProfile(String phoneNumber);
}
