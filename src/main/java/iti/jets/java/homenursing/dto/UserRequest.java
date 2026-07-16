package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.entity.enums.AccountType;
import iti.jets.java.homenursing.entity.enums.Gender;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private Gender gender;
    private AccountType accountType;
    private String profileImageUrl;
}
