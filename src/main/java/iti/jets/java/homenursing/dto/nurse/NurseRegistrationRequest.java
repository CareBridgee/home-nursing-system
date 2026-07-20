package iti.jets.java.homenursing.dto.nurse;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseRegistrationRequest {

    @NotNull
    private UUID userId;

    private String nationalId;
    private String nationalIdFrontUrl;
    private String nationalIdBackUrl;
    private String licenseImageUrl;
    private String professionalCertificateUrl;
    private String specialization;
    private Integer yearsOfExperience;
    private BigDecimal hourlyRate;
    private String bio;
}
