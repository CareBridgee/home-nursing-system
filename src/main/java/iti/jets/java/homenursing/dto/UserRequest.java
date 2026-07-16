package iti.jets.java.homenursing.dto;

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
    private String gender;
    private String accountType;
    private String profileImageUrl;
}
