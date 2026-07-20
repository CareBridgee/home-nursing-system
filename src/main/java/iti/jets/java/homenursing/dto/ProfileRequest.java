package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String bloodType;
    private BigDecimal height;
    private BigDecimal weight;
    private String mobilityStatus;
    private String mobilityNotes;
    private String previousSurgeries;
    private String previousHospitalizations;
}
