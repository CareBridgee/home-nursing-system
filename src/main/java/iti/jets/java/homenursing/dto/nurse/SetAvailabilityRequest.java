package iti.jets.java.homenursing.dto.nurse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetAvailabilityRequest {

    @NotEmpty
    private List<@Valid AvailabilitySlot> slots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AvailabilitySlot {

        @NotNull
        @Min(0)
        @Max(6)
        private Integer dayOfWeek;

        @NotNull
        private LocalTime startTime;

        @NotNull
        private LocalTime endTime;

        private Boolean isAvailable;
    }
}
