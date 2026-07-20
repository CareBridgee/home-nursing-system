package iti.jets.java.homenursing.dto.nurse;

import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseResponse {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String nationalId;
    private String nationalIdFrontUrl;
    private String nationalIdBackUrl;
    private String licenseImageUrl;
    private String professionalCertificateUrl;
    private String specialization;
    private Integer yearsOfExperience;
    private BigDecimal hourlyRate;
    private String bio;
    private BigDecimal ratingAvg;
    private Integer totalReviews;
    private Boolean isAvailable;
    private VerificationStatus verificationStatus;
    private String rejectionReason;
    private List<NurseServiceResponse> services;
}
