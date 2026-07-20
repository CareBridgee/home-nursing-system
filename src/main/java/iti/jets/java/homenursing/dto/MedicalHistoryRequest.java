package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.entity.enums.MedicalHistoryType;
import jakarta.validation.constraints.NotNull;
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
public class MedicalHistoryRequest {

    @NotNull
    private MedicalHistoryType type;

    @Size(max = 4000)
    private String description;
}
