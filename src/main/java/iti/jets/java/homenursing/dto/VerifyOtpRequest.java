package iti.jets.java.homenursing.dto;

import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyOtpRequest {
    @Pattern(regexp = "^\\+\\d{1,3}\\s?\\d{10,}$", message = "Valid phone number required")
    private String phoneNumber;

    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    private String otp;
}
