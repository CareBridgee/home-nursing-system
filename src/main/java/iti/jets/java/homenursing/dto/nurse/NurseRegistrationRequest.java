package iti.jets.java.homenursing.dto.nurse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseRegistrationRequest {

    @NotBlank(message = "National ID is required")
    @Pattern(regexp = "^\\d{14}$", message = "National ID must contain exactly 14 digits")
    private String nationalId;

    @NotBlank(message = "License number is required")
    @Size(max = 100, message = "License number must not exceed 100 characters")
    private String licenseNumber;

    @NotNull(message = "National ID front image is required")
    private MultipartFile nationalIdFront;

    @NotNull(message = "National ID back image is required")
    private MultipartFile nationalIdBack;

    @NotNull(message = "License image is required")
    private MultipartFile licenseImage;

    @NotNull(message = "Professional certificate is required")
    private MultipartFile professionalCertificate;

    @NotBlank(message = "Specialization is required")
    @Size(max = 255, message = "Specialization must not exceed 255 characters")
    private String specialization;

    @NotNull(message = "Years of experience is required")
    @PositiveOrZero(message = "Years of experience cannot be negative")
    private Integer yearsOfExperience;

    @NotNull(message = "Hourly rate is required")
    @Positive(message = "Hourly rate must be greater than zero")
    private BigDecimal hourlyRate;

    private String bio;
}
