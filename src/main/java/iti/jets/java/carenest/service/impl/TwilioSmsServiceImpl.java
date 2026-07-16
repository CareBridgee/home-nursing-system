package iti.jets.java.carenest.service.impl;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import iti.jets.java.carenest.service.TwilioSmsService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioSmsServiceImpl implements TwilioSmsService {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsServiceImpl.class);

    @Value("${TWILIO_ACCOUNT_SID:}")
    private String accountSid;

    @Value("${TWILIO_AUTH_TOKEN:}")
    private String authToken;

    @Value("${TWILIO_FROM_NUMBER:}")
    private String fromNumber;

    private boolean configured = false;

    @PostConstruct
    public void init() {
        if (!accountSid.isBlank() && !authToken.isBlank() && !fromNumber.isBlank()) {
            Twilio.init(accountSid, authToken);
            configured = true;
            log.info("Twilio SMS service initialized");
        } else {
            log.warn("Twilio not configured — OTPs will be logged to console for development");
        }
    }

    @Override
    public void sendOtp(String toNumber, String otp) {
        String messageBody = "Your care-nest verification code is: " + otp;

        if (!configured) {
            log.info("DEV OTP for {}: {}", toNumber, otp);
            return;
        }

        try {
            Message.creator(new PhoneNumber(toNumber), new PhoneNumber(fromNumber), messageBody).create();
            log.info("OTP sent to {}", toNumber);
        } catch (ApiException e) {
            log.error("Twilio error sending OTP to {}: {}", toNumber, e.getMessage());
            throw new RuntimeException("Failed to send SMS: " + e.getMessage());
        }
    }
}
