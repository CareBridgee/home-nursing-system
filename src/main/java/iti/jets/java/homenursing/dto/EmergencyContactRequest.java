package iti.jets.java.homenursing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContactRequest {

    @NotBlank
    @Size(max = 100)
    private String contactName;

    @Size(max = 100)
    private String relationship;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9\\s\\-]{7,20}$", message = "Invalid phone number format")
    private String phoneNumber;
}
