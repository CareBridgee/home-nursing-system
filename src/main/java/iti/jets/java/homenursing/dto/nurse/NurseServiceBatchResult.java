package iti.jets.java.homenursing.dto.nurse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseServiceBatchResult {

    private List<NurseServiceResponse> added;
    private List<BatchFailure> failed;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchFailure {
        private UUID serviceTypeId;
        private String reason;
    }
}