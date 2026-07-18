package iti.jets.java.homenursing.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationResponse {

    private UUID id;
    private String name;
    private String description;
}
