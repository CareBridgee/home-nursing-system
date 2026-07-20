package iti.jets.java.homenursing.dto.nurse;

import iti.jets.java.homenursing.entity.FailedStep;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseRejectionRequest {

    private String overallReason;

    @NotEmpty
    private List<FailedStep> failedSteps;
}
