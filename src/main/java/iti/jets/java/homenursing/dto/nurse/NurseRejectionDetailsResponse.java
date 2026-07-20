package iti.jets.java.homenursing.dto.nurse;

import iti.jets.java.homenursing.entity.FailedStep;

import java.util.List;

public record NurseRejectionDetailsResponse(
        String overallReason,
        List<FailedStep> failedSteps
) {
}
