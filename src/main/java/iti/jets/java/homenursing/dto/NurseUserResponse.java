package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.dto.nurse.NurseAuthResponse;
import iti.jets.java.homenursing.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseUserResponse {

    private UUID id;
    private String phoneNumber;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String profileImageUrl;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private UUID defaultProfileId;
    private NurseAuthResponse nurse;
}