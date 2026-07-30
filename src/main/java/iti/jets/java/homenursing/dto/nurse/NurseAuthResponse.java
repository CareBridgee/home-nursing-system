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
public class NurseAuthResponse {

    private UUID id;
    private String nationalId;
    private String nationalIdFrontUrl;
    private String nationalIdBackUrl;
    private String licenseImageUrl;
    private String professionalCertificateUrl;
    private String specialization;
    private Integer yearsOfExperience;
    private String bio;
    private BigDecimal ratingAvg;
    private Integer totalReviews;
    private VerificationStatus verificationStatus;
    private String rejectionReason;
    private NurseRejectionDetailsResponse rejectionDetails;
    private List<NurseServiceResponse> services;
}