package iti.jets.java.homenursing.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllergyResponse {

    private UUID id;
    private String name;
}
