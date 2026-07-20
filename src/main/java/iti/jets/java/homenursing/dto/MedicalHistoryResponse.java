package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.entity.enums.MedicalHistoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalHistoryResponse {

    private UUID id;
    private UUID profileId;
    private MedicalHistoryType type;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
