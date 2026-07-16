package iti.jets.java.carenest.service;

public interface TwilioSmsService {

    void sendOtp(String toNumber, String otp);
}
