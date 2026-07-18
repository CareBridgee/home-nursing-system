package iti.jets.java.homenursing.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceTypeResponse {

    private UUID id;
    private String name;
    private String description;
    private Integer estimatedDurationMinutes;
    private BigDecimal basePrice;
}
