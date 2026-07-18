package iti.jets.java.homenursing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceTypeRequest {

    @NotBlank
    private String name;

    private String nameAr;

    private String description;

    private String descriptionAr;

    private Integer estimatedDurationMinutes;

    private BigDecimal basePrice;
}
