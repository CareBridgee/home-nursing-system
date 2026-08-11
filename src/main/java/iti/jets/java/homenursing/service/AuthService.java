package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.auth.DevOtpResponse;
import iti.jets.java.homenursing.dto.auth.NurseTokenPair;
import iti.jets.java.homenursing.dto.auth.TokenPair;
import iti.jets.java.homenursing.dto.user.UserResponse;

public interface AuthService {

    void requestOtp(String phoneNumber);

    DevOtpResponse requestOtpDev(String phoneNumber);

    TokenPair verifyOtpAndLogin(String phoneNumber, String otp);

    NurseTokenPair verifyNurseOtpAndLogin(String phoneNumber, String otp);

    TokenPair refreshToken(String refreshToken);

    void logout(String refreshToken);

    UserResponse getUserProfile(String phoneNumber);
}
