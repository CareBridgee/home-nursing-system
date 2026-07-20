package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private UUID id;
    private UUID userId;
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
    private Boolean isPrimary;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
