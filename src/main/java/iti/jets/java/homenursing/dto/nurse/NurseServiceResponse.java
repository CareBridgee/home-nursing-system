package iti.jets.java.homenursing.dto.nurse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseServiceResponse {

    private UUID id;
    private UUID serviceTypeId;
    private String serviceName;
    private String serviceDescription;
    private BigDecimal basePrice;
    private BigDecimal customPrice;
    private Boolean isActive;
}
