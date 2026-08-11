package iti.jets.java.homenursing.dto.profile;

import iti.jets.java.homenursing.annotation.AllowedValues;
import iti.jets.java.homenursing.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRequest {

    private String relationship;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;
    @AllowedValues({"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"})
    private String bloodType;
    private BigDecimal height;
    private BigDecimal weight;
    @AllowedValues({"INDEPENDENT", "PARTIAL_ASSISTANCE", "TOTAL_ASSISTANCE", "WHEELCHAIR", "BEDRIDDEN"})
    private String mobilityStatus;
    private String mobilityNotes;
    private String previousSurgeries;
    private String previousHospitalizations;

    private String profileImageUrl;

    private MultipartFile profileImage;
}
