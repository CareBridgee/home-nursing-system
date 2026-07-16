package iti.jets.java.carenest.service;

import iti.jets.java.carenest.dto.TokenPair;
import iti.jets.java.carenest.dto.UserResponse;

public interface AuthService {

    void requestOtp(String phoneNumber);

    TokenPair verifyOtpAndLogin(String phoneNumber, String otp);

    TokenPair refreshToken(String refreshToken);

    void logout(String refreshToken);

    UserResponse getUserProfile(String phoneNumber);
}
