package iti.jets.java.homenursing.dto.nurse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateServicePriceRequest {

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal customPrice;
}
