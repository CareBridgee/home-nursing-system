package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.entity.enums.Gender;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRequest {

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String bloodType;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
}
