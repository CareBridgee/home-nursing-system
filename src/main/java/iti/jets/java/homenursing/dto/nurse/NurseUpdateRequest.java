package iti.jets.java.homenursing.dto.nurse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseUpdateRequest {

    private String nationalId;
    private String licenseNumber;
    private MultipartFile nationalIdFront;
    private MultipartFile nationalIdBack;
    private MultipartFile licenseImage;
    private MultipartFile professionalCertificate;
    private String specialization;
    private Integer yearsOfExperience;
    private BigDecimal hourlyRate;
    private String bio;
}
