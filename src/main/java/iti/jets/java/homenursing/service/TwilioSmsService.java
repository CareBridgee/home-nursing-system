package iti.jets.java.homenursing.service;

public interface TwilioSmsService {

    void sendOtp(String toNumber, String otp);
}
