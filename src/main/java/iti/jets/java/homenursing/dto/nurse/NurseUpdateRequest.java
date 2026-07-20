package iti.jets.java.homenursing.dto.nurse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseUpdateRequest {

    private String nationalId;
    private String nationalIdFrontUrl;
    private String nationalIdBackUrl;
    private String licenseImageUrl;
    private String professionalCertificateUrl;
    private String specialization;
    private Integer yearsOfExperience;
    private BigDecimal hourlyRate;
    private String bio;
    private Boolean isAvailable;
}
