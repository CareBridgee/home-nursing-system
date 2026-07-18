package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.entity.enums.AllergyType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllergyRequest {

    @NotBlank
    private String name;

    private String nameAr;

    private AllergyType type;
}
