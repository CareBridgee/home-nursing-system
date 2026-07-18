package iti.jets.java.homenursing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalConditionRequest {

    @NotBlank
    private String name;

    private String nameAr;

    private String description;

    private String descriptionAr;
}
